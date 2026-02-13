package com.portability.bot_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PortabilityRequest(
    @NotBlank(message = "The phone number can't be null")
    @Pattern(
        regexp = "^[0-9]{10,15}$",
        message = "The phone must be between 10 and 15 digits"
    )
    String phoneNumber,

    @NotBlank(message = "The order id can't be null")
    String orderId


) {}
