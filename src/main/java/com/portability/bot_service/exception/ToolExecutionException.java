package com.portability.bot_service.exception;

/**
 * Exception specifically designed for @Tool methods to provide clear,
 * user-friendly error messages to the LLM/chatbot while preserving
 * technical details for logging.
 */
public class ToolExecutionException extends RuntimeException {

    private final String toolName;
    private final String userFriendlyMessage;
    private final String technicalDetails;

    public ToolExecutionException(String toolName, String userFriendlyMessage, String technicalDetails) {
        super(String.format("[%s] %s", toolName, userFriendlyMessage));
        this.toolName = toolName;
        this.userFriendlyMessage = userFriendlyMessage;
        this.technicalDetails = technicalDetails;
    }

    public ToolExecutionException(String toolName, String userFriendlyMessage, Throwable cause) {
        super(String.format("[%s] %s", toolName, userFriendlyMessage), cause);
        this.toolName = toolName;
        this.userFriendlyMessage = userFriendlyMessage;
        this.technicalDetails = cause != null ? cause.getMessage() : "Unknown error";
    }

    public String getToolName() {
        return toolName;
    }

    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    /**
     * Returns a message formatted for the LLM to understand and communicate to the user
     */
    public String getMessageForLLM() {
        return String.format("La operación '%s' no pudo completarse: %s. Por favor, verifica los datos e intenta nuevamente.",
                toolName, userFriendlyMessage);
    }
}
