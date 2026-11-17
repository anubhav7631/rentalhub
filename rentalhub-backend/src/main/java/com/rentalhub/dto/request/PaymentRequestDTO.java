package com.rentalhub.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Rent month is required")
    @Min(1) @Max(12)
    private Integer rentMonth;

    @NotNull(message = "Rent year is required")
    @Min(2020) @Max(2100)
    private Integer rentYear;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotNull(message = "Tenant ID is required")
    private Long tenantId;

    private String paymentReference;
    private String notes;
}