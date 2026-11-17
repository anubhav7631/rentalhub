package com.rentalhub.repository;

import com.rentalhub.entity.Role;
import com.rentalhub.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Used in Phase 6 to assign roles during registration
    // and in Phase 8 for seeding default roles
    Optional<Role> findByName(RoleName name);
}