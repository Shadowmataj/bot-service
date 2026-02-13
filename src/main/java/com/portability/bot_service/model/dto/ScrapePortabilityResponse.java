package com.portability.bot_service.model.dto;

public record ScrapePortabilityResponse(
        Boolean portability_status,
        String message) {

}
