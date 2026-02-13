package com.portability.bot_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response of a SIM Card")
public record SimCardResponse(
    @Schema(description = "Unique ID of the SIM Card", example = "1")
    Long id,
    
    @Schema(description = "ICC number of the SIM Card", example = "89570000000000000001")
    String icc,
    
    @Schema(description = "Availability of the SIM Card", example = "true")
    Boolean available,
    
    @Schema(description = "Portability ID", example = "PORT-12345")
    String portabilityId,
    
    @Schema(description = "Type of SIM Card", example = "PHYSICAL")
    String simType,
    
    @Schema(description = "Name of the associated company", example = "Movistar")
    String companyName,
    
    @Schema(description = "ID of the associated product", example = "1")
    Long productId,
    
    @Schema(description = "Name of the associated product", example = "Basic Prepaid Plan")
    String productName
) {}
