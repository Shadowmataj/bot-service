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
public class AddressResponse {

    @Schema(description = "Unique identifier of the address", example = "1")
    private Long id;

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotBlank(message = "Street cannot be empty")
    @Size(max = 200, message = "Street cannot exceed 200 characters")
    private String street;

    @Size(max = 20, message = "Number cannot exceed 20 characters")
    private String number;

    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @Schema(description = "City of the address", example = "New York")
    private String city;

    @Schema(description = "State or province of the address", example = "NY")
    private String state;

    @NotBlank(message = "Postal code cannot be empty")
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Schema(description = "Country of the address", example = "USA")
    private String country;

    @Size(max = 500, message = "Additional information cannot exceed 500 characters")
    private String reference;

    private AddressType addressType;
}
