package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a checkout session")
public record CheckoutSessionRequest(
        
        @Schema(description = "Payment link identifier", example = "1")
        Long payment_link_id,
        
        @Schema(description = "Customer identifier", example = "1")
        Long customer_id,
        
        @Schema(description = "Order identifier", example = "1")
        String order_id,
        
        @Schema(description = "URL to redirect on successful payment", example = "https://example.com/success")
        String success_url,
        
        @Schema(description = "URL to redirect on payment cancellation", example = "https://example.com/cancel")
        String cancel_url
        ) {

}
