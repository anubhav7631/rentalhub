package com.rentalhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String profileImagePath;
    private Boolean isActive;
    private Boolean isEmailVerified;
    // Roles as simple strings — no nested Role objects
    private Set<String> roles;
    private LocalDateTime createdAt;
}