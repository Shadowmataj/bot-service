package com.portability.bot_service.model.dto;

import com.portability.bot_service.model.enm.AddressType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for Address")
public class AddressRequest {

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotBlank(message = "Street cannot be empty")
    @Size(max = 200, message = "Street cannot exceed 200 characters")
    private String street;

    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @Schema(description="Street number of the address", example="123A")
    private String number;

    @NotBlank(message = "Postal code cannot be empty")
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Size(max = 500, message = "Additional information cannot exceed 500 characters")
    private String reference;

    @NotNull(message = "Address type cannot be null")
    private AddressType addressType;
}
