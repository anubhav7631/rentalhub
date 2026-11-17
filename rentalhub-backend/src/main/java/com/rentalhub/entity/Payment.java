package com.rentalhub.entity;

import com.rentalhub.entity.enums.PaymentStatus;
import com.rentalhub.entity.listener.AuditEntityListener;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(
    name = "payments",
    uniqueConstraints = {
        
        @UniqueConstraint(
            name  = "uk_payment_property_tenant_month_year",
            columnNames = {"property_id", "tenant_id", "rent_month", "rent_year"}
        )
    },
    indexes = {
        @Index(name = "idx_payments_property",   columnList = "property_id"),
        @Index(name = "idx_payments_tenant",     columnList = "tenant_id"),
        @Index(name = "idx_payments_status",     columnList = "status"),
        @Index(name = "idx_payments_due_date",   columnList = "due_date"),
        @Index(name = "idx_payments_month_year", columnList = "rent_month, rent_year")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"property", "tenant", "recordedBy"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "rent_month", nullable = false)
    private Integer rentMonth;

    @Column(name = "rent_year", nullable = false)
    private Integer rentYear;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isPaid()    { return this.status == PaymentStatus.PAID;    }
    public boolean isOverdue() { return this.status == PaymentStatus.OVERDUE; }
    public boolean isPending() { return this.status == PaymentStatus.PENDING; }
}