package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request object for creating an order")
public record OrderRequest(
        @Schema(description = "Customer ID", example = "1")
        @NotNull(message = "Customer ID is required")
        Long customerId,
        @Schema(description = "Product ID", example = "1")
        @NotNull(message = "Product ID is required")
        Long productId,
        @Schema(description = "Address ID", example = "1")
        Long addressId,
        @Schema(description = "Checkout ID", example = "1")
        Long checkoutId
        ) {

}
