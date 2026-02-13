package com.portability.bot_service.model.dto;

public record ScrapePortabilityRequest(
        String phone_number,
        String imei,
        String portability_nip,
        String icc,
        String first_name,
        String last_name,
        String email) {

}
