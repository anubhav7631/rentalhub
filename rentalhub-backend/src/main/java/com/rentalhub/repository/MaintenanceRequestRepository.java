package com.rentalhub.repository;

import com.rentalhub.entity.MaintenanceRequest;
import com.rentalhub.entity.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    // Owner: all requests across their properties
    @EntityGraph(attributePaths = {"property", "tenant", "assignedTo", "images"})
    Page<MaintenanceRequest> findByPropertyOwnerIdOrderByCreatedAtDesc(
        Long ownerId, Pageable pageable
    );

    // Owner: filter by status
    @EntityGraph(attributePaths = {"property", "tenant", "images"})
    Page<MaintenanceRequest> findByPropertyOwnerIdAndStatusOrderByCreatedAtDesc(
        Long ownerId, MaintenanceStatus status, Pageable pageable
    );

    // Tenant: only their own requests
    @EntityGraph(attributePaths = {"property", "tenant", "images"})
    Page<MaintenanceRequest> findByTenantIdOrderByCreatedAtDesc(
        Long tenantId, Pageable pageable
    );

    // For a specific property
    @EntityGraph(attributePaths = {"tenant", "images"})
    Page<MaintenanceRequest> findByPropertyIdOrderByCreatedAtDesc(
        Long propertyId, Pageable pageable
    );

    // Count helpers for dashboards
    long countByPropertyOwnerIdAndStatus(Long ownerId, MaintenanceStatus status);
    long countByTenantIdAndStatus(Long tenantId, MaintenanceStatus status);
    long countByStatus(MaintenanceStatus status);

    // Open requests count for a property (dashboard tile)
    @Query("""
        SELECT COUNT(r) FROM MaintenanceRequest r
        WHERE r.property.id = :propertyId
        AND r.status IN ('OPEN', 'IN_PROGRESS')
        """)
    long countOpenByProperty(@Param("propertyId") Long propertyId);
}