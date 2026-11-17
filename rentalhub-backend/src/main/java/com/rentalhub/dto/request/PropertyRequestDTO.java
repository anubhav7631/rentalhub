package com.rentalhub.dto.request;

import com.rentalhub.entity.enums.PropertyType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRequestDTO {

    @NotBlank(message = "Property name is required")
    @Size(max = 200, message = "Property name too long")
    private String propertyName;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotBlank(message = "Address is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @NotNull(message = "Rent amount is required")
    @DecimalMin(value = "0.01", message = "Rent amount must be positive")
    private BigDecimal rentAmount;

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Deposit cannot be negative")
    private BigDecimal depositAmount;

    @Min(value = 1, message = "Bedrooms must be at least 1")
    private Integer bedrooms;

    @Min(value = 1, message = "Bathrooms must be at least 1")
    private Integer bathrooms;

    private BigDecimal areaSqft;
    private String description;
}