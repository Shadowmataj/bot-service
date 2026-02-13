package com.portability.bot_service.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ByEmailRequest {
    @Email
    @NotBlank
    private String email;
}
