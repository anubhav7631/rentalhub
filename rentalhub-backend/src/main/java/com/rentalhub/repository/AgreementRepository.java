package com.rentalhub.repository;

import com.rentalhub.entity.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    // All agreements for a specific property (most recent first)
    Page<Agreement> findByPropertyIdOrderByStartDateDesc(Long propertyId, Pageable pageable);

    // All agreements for a specific tenant
    List<Agreement> findByTenantIdOrderByStartDateDesc(Long tenantId);

    // Active agreement for a specific tenant — used by tenant dashboard
    Optional<Agreement> findByTenantIdAndIsActiveTrue(Long tenantId);

    // Active agreement for a specific property
    Optional<Agreement> findByPropertyIdAndIsActiveTrue(Long propertyId);

    // All agreements for an owner across all their properties
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.property.owner.id = :ownerId
        ORDER BY a.startDate DESC
        """)
    Page<Agreement> findByOwnerIdOrderByStartDateDesc(
        @Param("ownerId") Long ownerId,
        Pageable pageable
    );

    // Agreements expiring within the next N days (for @Scheduled alerts)
    @Query("""
        SELECT a FROM Agreement a
        WHERE a.isActive = true
        AND a.isExpired = false
        AND a.endDate BETWEEN :today AND :thresholdDate
        """)
    List<Agreement> findExpiringSoon(
        @Param("today") LocalDate today,
        @Param("thresholdDate") LocalDate thresholdDate
    );

    // Agreements whose end date has passed but not yet flagged as expired
    @Query("SELECT a FROM Agreement a WHERE a.endDate < :today AND a.isExpired = false")
    List<Agreement> findExpiredNotFlagged(@Param("today") LocalDate today);

    // Count agreements for number generation
    long countByAgreementNumberStartingWith(String prefix);
}