package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a checkout session")
public record CheckoutSessionBotRequest(
        @Schema(description = "Payment link identifier", example = "1")
        Long payment_link_id,
        @Schema(description = "Customer identifier", example = "1")
        Long customer_id,
        @Schema(description = "Portability identifier", example = "1")
        String order_id) {

}
