package com.rentalhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardDTO {

    // Properties
    private long totalProperties;
    private long occupiedProperties;
    private long vacantProperties;
    private int  occupancyRate;        // percentage 0-100

    // Tenants
    private long activeTenants;

    // Payments
    private BigDecimal collectedThisMonth;
    private BigDecimal pendingAmount;
    private BigDecimal overdueAmount;
    private long paidCount;
    private long pendingCount;
    private long overdueCount;

    // Agreements
    private long activeAgreements;
    private long expiringSoonCount;    // expiring within 30 days

    // Maintenance
    private long openRequests;
    private long inProgressRequests;

    // Charts
    private List<AdminDashboardDTO.MonthlyRevenuePoint> monthlyRevenue;  // last 6 months
    private List<PropertyOccupancyPoint> propertyOccupancy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyOccupancyPoint {
        private String propertyName;
        private String status;
        private BigDecimal rentAmount;
    }
}