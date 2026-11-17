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
public class PaymentSummaryDTO {
    private BigDecimal collectedThisMonth;
    private BigDecimal pendingThisMonth;
    private BigDecimal overdueTotal;
    private long paidCount;
    private long pendingCount;
    private long overdueCount;
    private int currentMonth;
    private int currentYear;
}