package com.rentalhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDashboardDTO {

    // My property
    private Long   propertyId;
    private String propertyName;
    private String propertyAddress;
    private String propertyCity;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;

    // My tenancy
    private Long   tenantId;
    private LocalDate moveInDate;
    private long   daysAsTenanant;

    // Current month rent
    private Long   currentPaymentId;
    private String currentPaymentStatus;  // PENDING / PAID / OVERDUE
    private BigDecimal currentPaymentAmount;
    private LocalDate  currentPaymentDueDate;
    private LocalDate  currentPaymentPaidDate;

    // Agreement
    private Long   agreementId;
    private String agreementStatus;       // ACTIVE / EXPIRING_SOON / EXPIRED / NONE
    private LocalDate agreementEndDate;
    private long   agreementDaysLeft;

    // Maintenance summary
    private long openMaintenanceRequests;
    private long inProgressRequests;
    private long totalMaintenanceRequests;
}