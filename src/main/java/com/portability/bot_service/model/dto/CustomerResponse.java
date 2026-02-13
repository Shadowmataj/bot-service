package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response with customer information")
public record CustomerResponse (
    @Schema(description = "Unique customer ID", example = "1")
    Long id,
    @Schema(description = "Customer's first name", example = "John")
    String firstName,
    @Schema(description = "Customer's last name", example = "Doe")
    String lastName,
    @Schema(description = "Customer's email address", example = "john.doe@email.com")
    String email,
    @Schema(description = "Customer's phone number", example = "555-1234")
    String phoneNumber
){}
