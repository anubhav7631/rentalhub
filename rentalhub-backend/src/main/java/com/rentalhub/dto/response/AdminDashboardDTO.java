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
public class AdminDashboardDTO {

    // Platform totals
    private long totalUsers;
    private long totalOwners;
    private long totalTenants;
    private long totalProperties;
    private long occupiedProperties;
    private long vacantProperties;

    // Financial
    private BigDecimal totalRevenueThisMonth;
    private BigDecimal totalRevenueThisYear;

    // Activity
    private long totalAgreements;
    private long activeAgreements;
    private long openMaintenanceRequests;
    private long totalMaintenanceRequests;

    // Monthly revenue chart — 12 months
    private List<MonthlyRevenuePoint> monthlyRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenuePoint {
        private String month;   // "Jan", "Feb", ...
        private BigDecimal amount;
    }
}