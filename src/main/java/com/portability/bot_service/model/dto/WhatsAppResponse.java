package com.portability.bot_service.model.dto;

/**
 * Response DTO for WhatsApp endpoint
 */
public class WhatsAppResponse {
    private String status;
    private String message;
    private int bufferedMessages;

    public WhatsAppResponse(String status, String message, int bufferedMessages) {
        this.status = status;
        this.message = message;
        this.bufferedMessages = bufferedMessages;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getBufferedMessages() {
        return bufferedMessages;
    }

    public void setBufferedMessages(int bufferedMessages) {
        this.bufferedMessages = bufferedMessages;
    }
}