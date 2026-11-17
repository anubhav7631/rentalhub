package com.rentalhub.repository;

import com.rentalhub.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    // Current tenant for a property
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.property.id = :propertyId
        AND t.moveOutDate IS NULL
        """)
    Optional<Tenant> findCurrentTenantByPropertyId(@Param("propertyId") Long propertyId);

    // Current tenants for an owner
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.property.owner.id = :ownerId
        AND t.moveOutDate IS NULL
        """)
    Page<Tenant> findCurrentTenantsByOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    // Check if property already has an active tenant
    boolean existsByPropertyIdAndMoveOutDateIsNull(Long propertyId);

    // Count current tenants for an owner
    @Query("""
        SELECT COUNT(t) FROM Tenant t
        WHERE t.property.owner.id = :ownerId
        AND t.moveOutDate IS NULL
        """)
    long countCurrentTenantsByOwner(@Param("ownerId") Long ownerId);

    // Count all current tenants (admin)
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.moveOutDate IS NULL")
    long countCurrentTenants();

    // History for a property
    List<Tenant> findByPropertyIdOrderByMoveInDateDesc(Long propertyId);

    // All tenants for an owner's properties (for reports)
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.property.owner.id = :ownerId
        ORDER BY t.property.propertyName, t.moveInDate DESC
        """)
    List<Tenant> findByPropertyOwnerId(@Param("ownerId") Long ownerId);

    // ── CRITICAL: use findFirst to avoid crash on duplicate active records ──
    Optional<Tenant> findFirstByTenantUserIdAndMoveOutDateIsNullOrderByMoveInDateDesc(
        Long userId
    );

    // Keep old one too in case anything still references it
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.tenantUser.id = :userId
        AND t.moveOutDate IS NULL
        ORDER BY t.moveInDate DESC
        """)
    Optional<Tenant> findByTenantUserIdAndMoveOutDateIsNull(@Param("userId") Long userId);
}