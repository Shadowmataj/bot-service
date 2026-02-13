package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing checkout session information")
public record CheckoutSessionResponse(
        @Schema(description = "Response message", example = "Checkout session created successfully")
        String message,
        @Schema(description = "Stripe checkout session URL", example = "https://checkout.stripe.com/c/pay/cs_test_1234567890")
        String stripe_session_url,
        @Schema(description = "Checkout session ID", example = "1")
        String checkout_session_id
        ) {

}
