package com.rentalhub.dto.response;

import com.rentalhub.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Long id;
    private BigDecimal amount;
    private Integer rentMonth;
    private Integer rentYear;
    private String monthLabel;   // "January 2024" — computed in service
    private LocalDate dueDate;
    private LocalDate paidDate;
    private PaymentStatus status;
    private BigDecimal lateFee;
    private String paymentReference;
    private String notes;
    private Long propertyId;
    private String propertyName;
    private Long tenantId;
    private String tenantName;
    private String recordedByName;
    private LocalDateTime createdAt;
}