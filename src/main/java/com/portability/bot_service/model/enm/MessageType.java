package com.portability.bot_service.model.enm;

/**
 * Types of messages in the conversation
 */
public enum MessageType {
    USER,       // Message from the user
    ASSISTANT,  // Response from the AI assistant
    SYSTEM,     // System message (state changes, etc.)
    TOOL        // Tool execution result
}
