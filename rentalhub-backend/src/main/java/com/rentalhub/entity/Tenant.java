package com.rentalhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.rentalhub.entity.listener.AuditEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditEntityListener.class)
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_property", columnList = "property_id"),
    @Index(name = "idx_tenants_email", columnList = "email"),
    @Index(name = "idx_tenants_tenant_user", columnList = "tenant_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"property", "tenantUser", "payments", "agreements", "maintenanceRequests"})
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "id_proof_number", length = 50)
    private String idProofNumber;

    @Column(name = "id_proof_type", length = 50)
    private String idProofType;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id")
    private User tenantUser;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<Payment> payments = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<Agreement> agreements = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<MaintenanceRequest> maintenanceRequests = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isCurrentTenant() {
        return this.moveOutDate == null;
    }
}