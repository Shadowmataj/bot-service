package com.portability.bot_service.model.dto;

public record ScrapeResponse(
    Boolean compatibility,
    String message
) {
    
}
