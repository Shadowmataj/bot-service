package com.portability.bot_service.service;

/**
 * ThreadLocal holder for the current conversation ID.
 * This allows tools and aspects to access the conversation context without
 * requiring it as a method parameter.
 */
public class ConversationContextHolder {

    private static final ThreadLocal<String> conversationIdHolder = new ThreadLocal<>();

    /**
     * Set the conversation ID for the current thread
     */
    public static void setConversationId(String conversationId) {
        conversationIdHolder.set(conversationId);
    }

    /**
     * Get the conversation ID for the current thread
     */
    public static String getConversationId() {
        return conversationIdHolder.get();
    }

    /**
     * Clear the conversation ID from the current thread
     */
    public static void clear() {
        conversationIdHolder.remove();
    }

    /**
     * Check if a conversation ID is set
     */
    public static boolean isSet() {
        return conversationIdHolder.get() != null;
    }
}
