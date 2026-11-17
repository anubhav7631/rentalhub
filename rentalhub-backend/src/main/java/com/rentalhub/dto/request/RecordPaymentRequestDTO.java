package com.rentalhub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordPaymentRequestDTO {

    @NotNull(message = "Paid date is required")
    private LocalDate paidDate;

    // Optional override — if null, uses the original amount
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    // UPI ID, cheque number, NEFT reference, cash, etc.
    private String paymentReference;

    private String notes;
}