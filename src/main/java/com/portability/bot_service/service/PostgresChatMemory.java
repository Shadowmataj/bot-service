package com.portability.bot_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portability.bot_service.model.enm.ConversationState;
import com.portability.bot_service.model.enm.MessageType;
import com.portability.bot_service.model.entity.ChatConversation;
import com.portability.bot_service.model.entity.ChatMessage;
import com.portability.bot_service.repository.ChatConversationRepository;
import com.portability.bot_service.repository.ChatMessageRepository;

/**
 * PostgreSQL-based implementation of ChatMemory for persistent conversation storage.
 * This enables state management and conversation history across sessions.
 */
@Service
public class PostgresChatMemory implements ChatMemory {

    private static final Logger logger = LoggerFactory.getLogger(PostgresChatMemory.class);
    private static final int MAX_MESSAGES_TO_LOAD = 50; // Limit messages for context window

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    public PostgresChatMemory(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        logger.debug("Adding {} messages to conversation: {}", messages.size(), conversationId);

        // Get or create conversation
        ChatConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseGet(() -> {
                    logger.info("Creating new conversation: {}", conversationId);
                    ChatConversation newConv = new ChatConversation(conversationId, conversationId);
                    return conversationRepository.save(newConv);
                });

        // Get current message count for ordering
        Integer maxOrder = messageRepository.findMaxMessageOrderByConversationId(conversationId);
        int currentOrder = (maxOrder != null) ? maxOrder + 1 : 0;

        // Save each message
        for (Message message : messages) {
            MessageType messageType = determineMessageType(message);
            ChatMessage chatMessage = new ChatMessage(
                    conversationId,
                    messageType,
                    message.getText(),
                    currentOrder++
            );

            // Add metadata if available
            if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
                chatMessage.setMetadata(message.getMetadata());
            }

            messageRepository.save(chatMessage);
        }

        // Update conversation timestamp
        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        logger.debug("Successfully added messages to conversation: {}", conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId) {
        logger.debug("Retrieving all messages from conversation: {}", conversationId);

        // Get all messages ordered by message order
        List<ChatMessage> chatMessages = messageRepository.findByConversationIdOrderByMessageOrderAsc(conversationId);

        // Convert to Spring AI Message objects
        List<Message> messages = chatMessages.stream()
                .map(this::convertToMessage)
                .collect(Collectors.toList());

        logger.debug("Retrieved {} messages from conversation: {}", messages.size(), conversationId);
        return messages;
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        logger.info("Clearing conversation: {}", conversationId);

        messageRepository.deleteByConversationId(conversationId);

        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversation -> {
                    conversation.setIsActive(false);
                    conversation.setCurrentState(ConversationState.ABANDONED);
                    conversationRepository.save(conversation);
                });

        logger.info("Successfully cleared conversation: {}", conversationId);
    }

    // Additional methods for state management

    @Transactional
    public void updateConversationState(String conversationId, ConversationState state) {
        logger.debug("Updating conversation {} state to: {}", conversationId, state);

        ChatConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation not found: " + conversationId));

        conversation.updateState(state);
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public ConversationState getConversationState(String conversationId) {
        return conversationRepository.findByConversationId(conversationId)
                .map(ChatConversation::getCurrentState)
                .orElse(ConversationState.INITIAL);
    }

    @Transactional
    public void addContextData(String conversationId, String key, Object value) {
        logger.debug("Adding context data to conversation {}: {} = {}", conversationId, key, value);

        ChatConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseGet(() -> {
                    ChatConversation newConv = new ChatConversation(conversationId, conversationId);
                    return conversationRepository.save(newConv);
                });

        conversation.addContextData(key, value);
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Object getContextData(String conversationId, String key) {
        return conversationRepository.findByConversationId(conversationId)
                .map(conv -> conv.getContextData(key))
                .orElse(null);
    }

    // Helper methods

    private MessageType determineMessageType(Message message) {
        if (message instanceof UserMessage) {
            return MessageType.USER;
        } else if (message instanceof AssistantMessage) {
            return MessageType.ASSISTANT;
        } else if (message instanceof SystemMessage) {
            return MessageType.SYSTEM;
        } else {
            return MessageType.TOOL;
        }
    }

    private Message convertToMessage(ChatMessage chatMessage) {
        String content = chatMessage.getContent();

        return switch (chatMessage.getMessageType()) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL -> new SystemMessage(content); // Tools as system messages
        };
    }
}
