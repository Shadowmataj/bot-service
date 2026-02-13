package com.portability.bot_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Imei {
    @NotBlank
    private String imei;
}
