package com.rentalhub.controller;

import com.rentalhub.dto.response.UserResponseDTO;
import com.rentalhub.entity.User;
import com.rentalhub.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@Slf4j
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(Pageable pageable) {
        log.debug("Admin fetching all users, page: {}", pageable.getPageNumber());

        Page<User> users = userRepository.findAll(pageable);
        Page<UserResponseDTO> response = users.map(this::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(this::toResponseDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, @NotNull Boolean> request) {

        Boolean isActive = request.get("isActive");
        log.info("Admin updating user {} active status to: {}", id, isActive);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new com.rentalhub.exception.ResourceNotFoundException("User", id));

        user.setIsActive(isActive);
        User saved = userRepository.save(user);

        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        return ResponseEntity.ok(Map.of(
            "totalAdmins", userRepository.countByRoleName(com.rentalhub.entity.enums.RoleName.ROLE_ADMIN),
            "totalOwners", userRepository.countByRoleName(com.rentalhub.entity.enums.RoleName.ROLE_OWNER),
            "totalTenants", userRepository.countByRoleName(com.rentalhub.entity.enums.RoleName.ROLE_TENANT)
        ));
    }

    private UserResponseDTO toResponseDTO(User user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());

        return UserResponseDTO.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .profileImagePath(user.getProfileImagePath())
            .isActive(user.getIsActive())
            .isEmailVerified(user.getIsEmailVerified())
            .roles(roleNames)
            .createdAt(user.getCreatedAt())
            .build();
    }
}