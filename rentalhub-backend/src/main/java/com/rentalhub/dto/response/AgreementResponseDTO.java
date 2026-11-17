package com.rentalhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementResponseDTO {
    private Long id;
    private String agreementNumber;
    private String filePath;
    private String fileName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
    private Boolean isActive;
    private Boolean isExpired;
    private String notes;

    // Property summary
    private Long propertyId;
    private String propertyName;
    private String propertyAddress;

    // Tenant summary
    private Long tenantId;
    private String tenantName;

    // Uploader
    private String uploadedByName;

    private LocalDateTime createdAt;

    // Computed fields (not stored in DB — calculated on the fly)
    private long daysUntilExpiry;
    private String expiryStatus; // "ACTIVE", "EXPIRING_SOON", "EXPIRED"
}