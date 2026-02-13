package com.portability.bot_service.model.dto;

import com.portability.bot_service.model.enm.PortabiliyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Portability response for the bot")
public class PortabilityResponse {

    @Schema(description = "Portability id.", example = "1")
    private Long id;
    @Schema(description = "Portability phone number.", example = "5555555555")
    private String phoneNumber;
    @Schema(description = "The customer imei.", example = "1234567891234567891F")
    private String imei;
    @Schema(description = "The customer potability nip.", example = "1234")
    private String portabilityNip;
    @Schema(description = "Portability status.", example = "STARTED")
    private PortabiliyStatus portabilityStatus;
    @Schema(description = "The order id.", example = "1")
    private String orderId;
}
