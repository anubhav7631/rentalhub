package com.rentalhub.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequestDTO {

    @NotBlank(message = "Tenant name is required")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email required")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Valid phone required")
    private String phone;

    private String idProofNumber;
    private String idProofType;

    @NotNull(message = "Move-in date is required")
    private LocalDate moveInDate;

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String notes;
}