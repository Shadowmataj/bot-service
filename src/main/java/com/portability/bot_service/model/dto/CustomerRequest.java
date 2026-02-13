package com.portability.bot_service.model.dto;

import org.springframework.context.annotation.Description;

@Description("Necessary data to register a new customer")
public record CustomerRequest(
    @Description("Custumer's first name")
    String firstName,
    @Description("Custumer's last name")
    String lastName,
    @Description("Custumer's email address")
    String email,
    @Description("Custumer's phone number")
    String phoneNumber
) {}
