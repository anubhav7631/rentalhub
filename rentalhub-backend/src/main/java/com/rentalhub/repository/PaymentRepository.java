package com.rentalhub.repository;

import com.rentalhub.entity.Payment;
import com.rentalhub.entity.enums.PaymentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"property", "tenant", "recordedBy"})
    Page<Payment> findByPropertyOwnerIdOrderByRentYearDescRentMonthDesc(
        Long ownerId, Pageable pageable
    );

    @EntityGraph(attributePaths = {"property", "tenant"})
    Page<Payment> findByPropertyIdOrderByRentYearDescRentMonthDesc(
        Long propertyId, Pageable pageable
    );

    @EntityGraph(attributePaths = {"property", "tenant"})
    Page<Payment> findByTenantIdOrderByRentYearDescRentMonthDesc(
        Long tenantId, Pageable pageable
    );

    // Duplicate check before inserting
    boolean existsByPropertyIdAndTenantIdAndRentMonthAndRentYear(
        Long propertyId, Long tenantId, Integer month, Integer year
    );

    // Find the specific payment record to check before marking paid
    Optional<Payment> findByPropertyIdAndTenantIdAndRentMonthAndRentYear(
        Long propertyId, Long tenantId, Integer month, Integer year
    );

    // Overdue detection — called by @Scheduled job daily
    @Query("""
        SELECT p FROM Payment p
        WHERE p.status = 'PENDING'
        AND p.dueDate < :today
        """)
    @EntityGraph(attributePaths = {"property", "tenant"})
    List<Payment> findPendingOverdue(@Param("today") LocalDate today);

    // All payments for an owner filtered by status
    List<Payment> findByPropertyOwnerIdAndStatus(
        Long ownerId, PaymentStatus status
    );

    // All payments for an owner in a specific year
    @Query("""
        SELECT p FROM Payment p
        WHERE p.property.owner.id = :ownerId
        AND p.rentYear = :year
        ORDER BY p.rentMonth, p.property.propertyName
        """)
    List<Payment> findByPropertyOwnerIdAndYear(
        @Param("ownerId") Long ownerId,
        @Param("year") Integer year
    );

    // Count helpers for dashboard
    long countByPropertyOwnerIdAndStatus(
        Long ownerId, PaymentStatus status
    );

    // Revenue aggregates (defined in Phase 12 full version)
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.property.owner.id = :ownerId
        AND p.status = 'PAID'
        AND p.rentMonth = :month
        AND p.rentYear = :year
        """)
    BigDecimal sumPaidByOwnerAndMonth(
        @Param("ownerId") Long ownerId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.property.owner.id = :ownerId
        AND p.status = 'PENDING'
        """)
    BigDecimal sumPendingByOwner(
        @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = 'PAID'
        AND p.rentMonth = :month
        AND p.rentYear = :year
        """)
    BigDecimal sumPaidForMonth(
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("""
        SELECT p.rentMonth, COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.property.owner.id = :ownerId
        AND p.status = 'PAID'
        AND p.rentYear = :year
        GROUP BY p.rentMonth
        ORDER BY p.rentMonth
        """)
    List<Object[]> monthlyRevenueByOwnerAndYear(
        @Param("ownerId") Long ownerId,
        @Param("year") Integer year
    );

    // For @Scheduled: all active tenants to generate monthly records
    @Query("""
        SELECT p FROM Payment p
        WHERE p.property.owner.id = :ownerId
        AND p.rentYear = :year
        AND p.rentMonth = :month
        """)
    List<Payment> findByOwnerAndMonth(
        @Param("ownerId") Long ownerId,
        @Param("year") Integer year,
        @Param("month") Integer month
    );
}