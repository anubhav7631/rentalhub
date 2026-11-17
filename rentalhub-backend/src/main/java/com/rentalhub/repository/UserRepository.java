package com.rentalhub.repository;

import com.rentalhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used in Phase 6: load user by email for authentication
    Optional<User> findByEmail(String email);

    // Used in Phase 6: check email uniqueness during registration
    boolean existsByEmail(String email);

    // Load all active users (for Admin user management)
    List<User> findByIsActiveTrue();

    // Find by reset token — Phase 18 password reset
    Optional<User> findByPasswordResetToken(String token);
    
    

    // Find users with a specific role — useful for admin dashboards
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true")
    List<User> findByRoleName(
        @org.springframework.data.repository.query.Param("roleName")
        com.rentalhub.entity.enums.RoleName roleName
    );

    // Count active users by role — for Admin dashboard stats
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isActive = true")
    long countByRoleName(
        @org.springframework.data.repository.query.Param("roleName")
        com.rentalhub.entity.enums.RoleName roleName
    );
}