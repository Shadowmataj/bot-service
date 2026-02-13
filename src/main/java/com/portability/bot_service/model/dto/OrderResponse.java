package com.portability.bot_service.model.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object for order details")
public record OrderResponse(
    @Schema(description = "Order ID", example = "P17379827490001")
    String id,
    
    @Schema(description = "Customer ID", example = "1")
    Long customerId,
    
    @Schema(description = "Product ID", example = "1")
    Long productId,
    
    @Schema(description = "SIM card ID", example = "1")
    Long simId,
    
    @Schema(description = "Address ID", example = "1")
    Long addressId,
    
    @Schema(description = "Checkout ID", example = "1")
    Long checkoutId,
    
    @Schema(description = "Payment ID", example = "1")
    Long paymentId,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt
) {
}
