package com.portability.bot_service.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portability.bot_service.model.enm.ConversationState;
import com.portability.bot_service.model.entity.ChatConversation;
import com.portability.bot_service.repository.ChatConversationRepository;

/**
 * Service for managing conversation states and transitions.
 * This provides the foundation for implementing a state machine
 * to control the conversation flow.
 * 
 * Future enhancements:
 * - Implement Spring State Machine or custom FSM
 * - Add transition guards and actions
 * - Implement event-driven state changes
 * - Add state persistence and recovery
 */
@Service
public class ConversationStateService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationStateService.class);

    private final ChatConversationRepository conversationRepository;
    private final PostgresChatMemory chatMemory;

    public ConversationStateService(
            ChatConversationRepository conversationRepository,
            PostgresChatMemory chatMemory) {
        this.conversationRepository = conversationRepository;
        this.chatMemory = chatMemory;
    }

    /**
     * Get the current state of a conversation
     */
    @Transactional(readOnly = true)
    public ConversationState getCurrentState(String conversationId) {
        return conversationRepository.findByConversationId(conversationId)
                .map(ChatConversation::getCurrentState)
                .orElse(ConversationState.INITIAL);
    }

    /**
     * Transition to a new state with validation
     */
    @Transactional
    public boolean transitionTo(String conversationId, ConversationState newState) {
        logger.info("Attempting state transition for {}: -> {}", conversationId, newState);

        ChatConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseGet(() -> {
                    ChatConversation newConv = new ChatConversation(conversationId, conversationId);
                    return conversationRepository.save(newConv);
                });

        ConversationState currentState = conversation.getCurrentState();

        // Validate transition
        if (!isValidTransition(currentState, newState)) {
            logger.warn("Invalid state transition from {} to {} for conversation {}",
                    currentState, newState, conversationId);
            return false;
        }

        // Perform transition
        conversation.updateState(newState);
        conversationRepository.save(conversation);

        logger.info("State transition successful for {}: {} -> {}",
                conversationId, currentState, newState);

        return true;
    }

    /**
     * Store conversation context data
     */
    @Transactional
    public void storeContextData(String conversationId, String key, Object value) {
        chatMemory.addContextData(conversationId, key, value);
    }

    /**
     * Retrieve conversation context data
     */
    @Transactional(readOnly = true)
    public Object getContextData(String conversationId, String key) {
        return chatMemory.getContextData(conversationId, key);
    }

    /**
     * Store multiple context values at once
     */
    @Transactional
    public void storeContextData(String conversationId, Map<String, Object> contextData) {
        contextData.forEach((key, value) -> chatMemory.addContextData(conversationId, key, value));
    }

    /**
     * Get all context data for a conversation
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllContextData(String conversationId) {
        return conversationRepository.findByConversationId(conversationId)
                .map(ChatConversation::getContextData)
                .orElse(new HashMap<>());
    }

    /**
     * Reset conversation to initial state
     */
    @Transactional
    public void resetConversation(String conversationId) {
        logger.info("Resetting conversation: {}", conversationId);

        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversation -> {
                    conversation.updateState(ConversationState.INITIAL);
                    conversation.setContextData(new HashMap<>());
                    conversationRepository.save(conversation);
                });

        chatMemory.clear(conversationId);
    }

    /**
     * Clear error context to allow retry of failed operations
     * This removes error-related context data without clearing the entire conversation
     */
    @Transactional
    public void clearErrorContext(String conversationId) {
        logger.info("Clearing error context for conversation: {}", conversationId);
        
        // Get current context data
        Map<String, Object> contextData = getAllContextData(conversationId);
        
        // Remove error-related keys
        contextData.remove("last_error");
        contextData.remove("error_timestamp");
        contextData.remove("failed_tool");
        contextData.remove("error_count");
        
        // Update context
        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversation -> {
                    conversation.setContextData(contextData);
                    conversationRepository.save(conversation);
                });
        
        logger.info("Error context cleared for conversation: {}", conversationId);
    }

    /**
     * Record error information in context for tracking
     */
    @Transactional
    public void recordError(String conversationId, String toolName, String errorMessage) {
        logger.info("Recording error for conversation {}: tool={}, message={}", 
                conversationId, toolName, errorMessage);
        
        Map<String, Object> errorContext = new HashMap<>();
        errorContext.put("last_error", errorMessage);
        errorContext.put("error_timestamp", System.currentTimeMillis());
        errorContext.put("failed_tool", toolName);
        
        // Increment error count
        Object errorCountObj = getContextData(conversationId, "error_count");
        int errorCount = errorCountObj != null ? (int) errorCountObj + 1 : 1;
        errorContext.put("error_count", errorCount);
        
        storeContextData(conversationId, errorContext);
    }

    /**
     * Check if conversation is in error state and user is attempting retry
     * This helps detect retry intentions to clear error context
     */
    @Transactional(readOnly = true)
    public boolean isRetryAttempt(String conversationId) {
        ConversationState currentState = getCurrentState(conversationId);
        Object lastError = getContextData(conversationId, "last_error");
        
        // If there's an error recorded and we're in error state
        return currentState == ConversationState.ERROR_STATE && lastError != null;
    }

    /**
     * Transition from error state back to the previous valid state
     * This allows the conversation to continue from where it failed
     */
    @Transactional
    public boolean recoverFromError(String conversationId, ConversationState targetState) {
        logger.info("Attempting to recover conversation {} from error to state: {}", 
                conversationId, targetState);
        
        // Clear error context
        clearErrorContext(conversationId);
        
        // Transition to target state
        boolean transitioned = transitionTo(conversationId, targetState);
        
        if (transitioned) {
            logger.info("Successfully recovered conversation {} to state: {}", 
                    conversationId, targetState);
        } else {
            logger.warn("Failed to recover conversation {} to state: {}", 
                    conversationId, targetState);
        }
        
        return transitioned;
    }

    /**
     * Validates if a state transition is allowed.
     * Uses a progressive ordinal-based approach to allow forward movement in the conversation flow.
     * This is more flexible than strict transitions and matches the dynamic nature of chatbot conversations.
     */
    public boolean isValidTransition(ConversationState from, ConversationState to) {
        // Allow any transition from ERROR_STATE or INITIAL (for recovery and restart)
        if (from == ConversationState.ERROR_STATE || from == ConversationState.INITIAL) {
            return true;
        }

        // Allow backward transitions to ERROR_STATE or BLOCKED
        if (to == ConversationState.ERROR_STATE || to == ConversationState.BLOCKED) {
            return true;
        }

        // Allow staying in the same state (for retries)
        if (from == to) {
            return true;
        }

        // Define state progression order (lower ordinal = earlier in flow)
        int fromOrdinal = getStateOrdinal(from);
        int toOrdinal = getStateOrdinal(to);
        
        // Special cases first
        if (from == ConversationState.COMPLETED || from == ConversationState.ABANDONED) {
            // Can restart from these states
            return to == ConversationState.INITIAL || to == ConversationState.INTENT_SELECTION;
        }
        
        if (from == ConversationState.BLOCKED) {
            // Can reactivate from blocked
            return to == ConversationState.INITIAL || to == ConversationState.INTENT_SELECTION;
        }
        
        // Allow forward progression (can skip states)
        // Example: INTENT_SELECTION (10) -> PAYMENT_PENDING (50) is valid
        if (toOrdinal > fromOrdinal) {
            return true;
        }
        
        // Allow some backward movements for natural conversation flow
        // Example: Going back from PAYMENT_PENDING to ADDRESS_REQUIRED to fix address
        if (toOrdinal >= fromOrdinal - 20) { // Allow going back up to 2 major steps
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns an ordinal value for each state to determine progression order.
     * Lower values are earlier in the conversation flow.
     */
    private int getStateOrdinal(ConversationState state) {
        return switch (state) {
            case INITIAL -> 0;
            case CUSTOMER_REGISTRATION -> 5;
            case INTENT_SELECTION -> 10;
            case PRODUCT_SELECTED -> 20;
            case IMEI_REQUIRED -> 30;
            case IMEI_VALIDATED -> 35;
            case ADDRESS_REQUIRED -> 40;
            case PAYMENT_PENDING -> 50;
            case PAYMENT_CONFIRMED -> 60;
            case SIM_SHIPPED -> 70;
            case PORTABILITY_WAIT_SIM -> 80;
            case PORTABILITY_NIP_REQUIRED -> 85;
            case PORTABILITY_SIM_ACTIVATION -> 90;
            case PORTABILITY_IN_PROGRESS -> 95;
            case PORTABILITY_COMPLETED -> 100;
            case COMPLETED -> 110;
            case BLOCKED -> 900;
            case ERROR_STATE -> 950;
            case ABANDONED -> 999;
        };
    }

    /**
     * Get conversation statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConversationStats(String conversationId) {
        Map<String, Object> stats = new HashMap<>();

        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversation -> {
                    stats.put("conversationId", conversation.getConversationId());
                    stats.put("currentState", conversation.getCurrentState());
                    stats.put("createdAt", conversation.getCreatedAt());
                    stats.put("updatedAt", conversation.getUpdatedAt());
                    stats.put("isActive", conversation.getIsActive());
                    stats.put("messageCount", conversation.getMessages().size());
                });

        return stats;
    }
}
