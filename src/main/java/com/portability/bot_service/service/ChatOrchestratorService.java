package com.portability.bot_service.service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.portability.bot_service.exception.ToolExecutionException;
import com.portability.bot_service.model.enm.ConversationState;
import com.portability.bot_service.tools.AddressesTools;
import com.portability.bot_service.tools.CustomerTools;
import com.portability.bot_service.tools.OrderTools;
import com.portability.bot_service.tools.PaymentTools;
import com.portability.bot_service.tools.ScraperTools;

/**
 * Orchestrator service that handles the conversation flow with tool call management.
 * This service processes user messages through a controlled loop that:
 * 1. Sends message to ChatClient
 * 2. Processes tool calls if present
 * 3. Returns results back to ChatClient
 * 4. Repeats until no more tool calls (max 5 iterations)
 */
@Service
public class ChatOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(ChatOrchestratorService.class);
    private static final int MAX_TOOL_ITERATIONS = 2;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ConversationStateService stateService;
    private final ResourceLoader resourceLoader;
    private final VectorStore vectorStore;
    private final ContextEnricher contextEnricher;
    
    // Tools
    private final CustomerTools customerTools;
    private final OrderTools orderTools;
    private final PaymentTools paymentTools;
    private final ScraperTools scraperTools;
    private final AddressesTools addressesTools;

    public ChatOrchestratorService(
            ChatClient chatClient,
            ChatMemory chatMemory,
            ConversationStateService stateService,
            ResourceLoader resourceLoader,
            VectorStore vectorStore,
            ContextEnricher contextEnricher,
            CustomerTools customerTools,
            OrderTools orderTools,
            PaymentTools paymentTools,
            ScraperTools scraperTools,
            AddressesTools addressesTools) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.stateService = stateService;
        this.resourceLoader = resourceLoader;
        this.vectorStore = vectorStore;
        this.contextEnricher = contextEnricher;
        this.customerTools = customerTools;
        this.orderTools = orderTools;
        this.paymentTools = paymentTools;
        this.scraperTools = scraperTools;
        this.addressesTools = addressesTools;
    }

    /**
     * Main entry point for handling user messages with tool execution loop
     */
    public String handleMessage(String userMessage, String phoneNumber) {
        try {
            // Set conversation context for the current thread
            ConversationContextHolder.setConversationId(phoneNumber);
            
            // Get current conversation state
            ConversationState currentState = stateService.getCurrentState(phoneNumber);
            logger.info("Processing message for user {} in state: {}", phoneNumber, currentState);

            // Check if this is a retry attempt after an error
            if (stateService.isRetryAttempt(phoneNumber)) {
                logger.info("Detected retry attempt for conversation: {}", phoneNumber);
                // Clear error context to allow fresh execution
                stateService.clearErrorContext(phoneNumber);
                // Get the state before error to restore proper flow
                currentState = stateService.getCurrentState(phoneNumber);
                logger.info("Cleared error context, current state: {}", currentState);
            }

            // Load system prompt with context
            String systemPrompt = buildSystemPrompt(phoneNumber, userMessage, currentState);

            // Process message with tool execution loop
            String response = processWithToolLoop(systemPrompt, userMessage, phoneNumber);

            // Update conversation state based on response (extracts state from marker)
            updateStateBasedOnResponse(phoneNumber, userMessage, response);

            // Clean state marker before returning to user
            return cleanStateMarker(response);

        } catch (ToolExecutionException e) {
            logger.error("Tool execution failed: {} - {}", e.getToolName(), e.getTechnicalDetails(), e);
            
            // Record error for tracking and retry detection
            stateService.recordError(phoneNumber, e.getToolName(), e.getMessageForLLM());
            
            // Transition to error state only if not already there
            ConversationState currentState = stateService.getCurrentState(phoneNumber);
            if (currentState != ConversationState.ERROR_STATE) {
                stateService.transitionTo(phoneNumber, ConversationState.ERROR_STATE);
            }
            
            return e.getMessageForLLM();

        } catch (Exception e) {
            logger.error("Unexpected error during message processing", e);
            
            // Record generic error
            stateService.recordError(phoneNumber, "system", "Unexpected error occurred");
            stateService.transitionTo(phoneNumber, ConversationState.ERROR_STATE);
            
            return "Lo siento, ocurrió un error inesperado. Por favor, intenta nuevamente o reformula tu pregunta.";
            
        } finally {
            // Always clear the conversation context from ThreadLocal
            ConversationContextHolder.clear();
        }
    }

    /**
     * Process message with tool execution loop (max 5 iterations)
     */
    private String processWithToolLoop(String systemPrompt, String userMessage, String phoneNumber) {
        int iteration = 0;
        ChatResponse response;

        // Initial call with user message
        response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(phoneNumber)
                        .build())
                .tools(customerTools, orderTools, paymentTools, scraperTools, addressesTools)
                .call()
                .chatResponse();

        // Process tool calls in a loop
        while (hasToolCalls(response) && iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            logger.info("Tool execution iteration {} for conversation {}", iteration, phoneNumber);

            try {
                // Get tool call results
                String toolResults = extractToolResults(response);
                
                // Continue conversation with tool results
                response = chatClient.prompt()
                        .system(systemPrompt)
                        .user("Resultado de las herramientas: " + toolResults)
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(phoneNumber)
                                .build())
                        .tools(customerTools, orderTools, paymentTools, scraperTools, addressesTools)
                        .call()
                        .chatResponse();

            } catch (Exception e) {
                logger.error("Error in tool execution iteration {}: {}", iteration, e.getMessage(), e);
                break;
            }
        }

        if (iteration >= MAX_TOOL_ITERATIONS) {
            logger.warn("Reached maximum tool iterations ({}) for conversation {}", MAX_TOOL_ITERATIONS, phoneNumber);
        }

        // Return final content
        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
            return response.getResult().getOutput().getText();
        }
        
        logger.warn("Empty response from ChatClient for conversation {}", phoneNumber);
        return "Lo siento, no pude procesar tu mensaje. Por favor, intenta de nuevo.";
    }

    /**
     * Build system prompt with RAG context, conversation state, and available context data
     */
    private String buildSystemPrompt(String phoneNumber, String userQuery, ConversationState currentState) throws IOException {
        String promptTemplate = Files.readString(
                resourceLoader.getResource("classpath:prompts/chatbot-rag-prompt.st")
                        .getFile().toPath());
        String context = fetchSemanticContext(userQuery);
        
        // Generate context summary from stored conversation data
        String contextSummary = contextEnricher.generateContextSummary(phoneNumber);

        Map<String, Object> variables = new HashMap<>();
        variables.put("context", context);
        variables.put("phoneNumber", phoneNumber);
        variables.put("userQuery", userQuery);
        variables.put("conversationState", currentState.toString());
        variables.put("availableData", contextSummary);

        PromptTemplate prompt = PromptTemplate.builder()
                .template(promptTemplate)
                .variables(variables)
                .build();

        return prompt.render();
    }

    /**
     * Fetch semantic context from vector store using RAG
     */
    private String fetchSemanticContext(String userQuery) {
        try {
            logger.info("[RAG] Fetching semantic context for query: {}", userQuery);
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(userQuery)
                            .topK(5)
                            .similarityThreshold(0.5f)
                            .build());

            logger.info("[RAG] Retrieved {} documents from vector store", documents.size());
            
            if (documents.isEmpty()) {
                logger.warn("[RAG] No documents found in vector store for query: {}", userQuery);
                return "No relevant information found in the knowledge base for this query.";
            }

            StringBuilder context = new StringBuilder();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                // Use getText() to get the content that was passed as second parameter to Document constructor
                String content = doc.getText();
                logger.info("[RAG] Document {}: {}", i + 1, content != null && content.length() > 150 ? content.substring(0, 150) + "..." : content);
                if (content != null && !content.isBlank()) {
                    context.append(content).append("\n\n");
                }
            }
            
            String finalContext = context.toString();
            logger.info("[RAG] Total context length: {} characters", finalContext.length());
            return finalContext;
        } catch (Exception e) {
            logger.error("[RAG] Error fetching semantic context", e);
            return "Context retrieval temporarily unavailable.";
        }
    }

    /**
     * Check if response has tool calls
     */
    private boolean hasToolCalls(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return false;
        }

        AssistantMessage output = response.getResult().getOutput();
        return output.getToolCalls() != null && !output.getToolCalls().isEmpty();
    }

    /**
     * Extract tool call results from response
     */
    private String extractToolResults(ChatResponse response) {
        StringBuilder results = new StringBuilder();
        
        AssistantMessage output = response.getResult().getOutput();
        if (output.getToolCalls() != null) {
            output.getToolCalls().forEach(toolCall -> {
                results.append("Tool: ").append(toolCall.name())
                       .append(", Result: ").append(toolCall.id())
                       .append("\n");
            });
        }

        return results.toString();
    }

    /**
     * Update conversation state based on the interaction
     * Extracts state from the assistant's response marker
     */
    private void updateStateBasedOnResponse(String conversationId, String userMessage, String assistantResponse) {
        try {
            ConversationState currentState = stateService.getCurrentState(conversationId);
            ConversationState newState = extractStateFromResponse(assistantResponse, currentState);

            if (newState != null && newState != currentState) {
                boolean transitioned = stateService.transitionTo(conversationId, newState);
                if (transitioned) {
                    logger.info("State transition for {}: {} -> {}", conversationId, currentState, newState);
                } else {
                    logger.warn("Invalid state transition attempted: {} -> {}", currentState, newState);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating conversation state", e);
        }
    }

    /**
     * Extract state from assistant response using the [STATE:STATE_NAME] marker
     */
    private ConversationState extractStateFromResponse(String assistantResponse, ConversationState currentState) {
        if (assistantResponse == null || assistantResponse.trim().isEmpty()) {
            return null;
        }

        // Look for [STATE:STATE_NAME] pattern - find the LAST occurrence
        String pattern = "\\[STATE:([A-Z_]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(assistantResponse);
        
        String lastStateName = null;
        while (m.find()) {
            lastStateName = m.group(1);
        }
        
        if (lastStateName != null) {
            try {
                ConversationState detectedState = ConversationState.valueOf(lastStateName);
                logger.info("Extracted state marker from response: {} (current: {})", detectedState, currentState);
                return detectedState;
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid state name in response marker: {}", lastStateName);
                return null;
            }
        }
        
        logger.debug("No state marker found in response, keeping current state: {}", currentState);
        return null;
    }

    /**
     * Remove state marker from assistant response before returning to user
     */
    private String cleanStateMarker(String response) {
        if (response == null) {
            return null;
        }
        return response.replaceAll("\\[STATE:[A-Z_]+\\]\\s*$", "").trim();
    }
}
