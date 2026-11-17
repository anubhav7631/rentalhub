package com.rentalhub.repository;

import com.rentalhub.entity.Property;
import com.rentalhub.entity.enums.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // All properties belonging to a specific owner (with pagination)
    // Pageable is Spring Data's pagination object — it carries:
    //   page number, page size, and sort order.
    // Returns Page<Property> which includes total count + current page data.
    Page<Property> findByOwnerId(Long ownerId, Pageable pageable);

    // Filter by status for a specific owner
    List<Property> findByOwnerIdAndStatus(Long ownerId, PropertyStatus status);

    // Count by status for dashboard stats
    long countByStatus(PropertyStatus status);
    long countByOwnerIdAndStatus(Long ownerId, PropertyStatus status);
 // Count all properties for a specific owner (already exists — verify)
    long countByOwnerId(Long ownerId);
    // Search across city, state, property name — used in Phase 9 search
    // JPQL LOWER() for case-insensitive search
    @Query("""
        SELECT p FROM Property p
        WHERE p.owner.id = :ownerId
        AND (
            LOWER(p.propertyName)  LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.city)          LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.addressLine1)  LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """)
    Page<Property> searchByOwner(
        @Param("ownerId") Long ownerId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    // Admin: search across all properties
    @Query("""
        SELECT p FROM Property p
        WHERE LOWER(p.propertyName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.city)          LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.state)         LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<Property> searchAll(@Param("keyword") String keyword, Pageable pageable);
}