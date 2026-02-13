package com.portability.bot_service.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main chat service that delegates to the orchestrator for message processing.
 * This service maintains backward compatibility with existing API.
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatOrchestratorService orchestrator;

    /**
     * Process user message and return bot response.
     * Delegates to orchestrator for tool execution loop management.
     */
    public String getBotResponse(String message, String phoneNumber) throws IOException {
        logger.info("Received message from {}: {}", phoneNumber, message);
        
        try {
            return orchestrator.handleMessage(message, phoneNumber);
        } catch (Exception e) {
            logger.error("Error processing message for {}: {}", phoneNumber, e.getMessage(), e);
            throw new IOException("Failed to process message", e);
        }
    }
}
