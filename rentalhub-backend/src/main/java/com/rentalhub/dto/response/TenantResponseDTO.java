package com.rentalhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String idProofNumber;
    private String idProofType;
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private Boolean isCurrentTenant;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String notes;
    private Long propertyId;
    private String propertyName;
    private String propertyAddress;
    private LocalDateTime createdAt;
}