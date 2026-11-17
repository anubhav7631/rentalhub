package com.rentalhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    // Owner / Admin stats
    private long totalProperties;
    private long occupiedProperties;
    private long vacantProperties;
    private long totalTenants;
    private long openMaintenanceRequests;
    private BigDecimal monthlyRevenue;      // sum of PAID payments this month
    private BigDecimal pendingRevenue;      // sum of PENDING payments

    // Admin-only extras
    private long totalUsers;
    private long totalOwners;
}