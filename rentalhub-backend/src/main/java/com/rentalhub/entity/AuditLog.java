package com.rentalhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity",    columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_user",      columnList = "performed_by_user_id"),
        @Index(name = "idx_audit_action",    columnList = "action"),
        @Index(name = "idx_audit_performed", columnList = "performed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String action;        // CREATE, UPDATE, DELETE

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;    // "Property", "Payment", "Tenant", etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "performed_at")
    private LocalDateTime performedAt;

    @Column(columnDefinition = "TEXT")
    private String details;       // Optional JSON snapshot
}