package com.rentalhub.service;

import com.rentalhub.dto.request.LoginRequestDTO;
import com.rentalhub.dto.request.RefreshTokenRequestDTO;
import com.rentalhub.dto.request.UserRequestDTO;
import com.rentalhub.dto.response.AuthResponseDTO;
import com.rentalhub.entity.RefreshToken;
import com.rentalhub.entity.Role;
import com.rentalhub.entity.User;
import com.rentalhub.entity.enums.RoleName;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.RoleRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.JwtService;
import com.rentalhub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final RefreshTokenService   refreshTokenService;

    @Transactional
    public AuthResponseDTO register(UserRequestDTO request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address is already registered");
        }

        RoleName requestedRole;
        try {
            requestedRole = RoleName.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + request.getRole());
        }

        if (requestedRole == RoleName.ROLE_ADMIN) {
            throw new BadRequestException("Cannot self-register as Admin");
        }

        Role role = roleRepository.findByName(requestedRole)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Role not found. Ensure roles are seeded."));

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail().toLowerCase().trim())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .isActive(true)
            .isEmailVerified(false)
            .build();

        user.addRole(role);
        User savedUser = userRepository.save(user);

        // ── PHASE 7 ADDITION: Generate real JWT tokens ────────────
        UserPrincipal principal = UserPrincipal.create(savedUser);
        String accessToken  = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        // Persist the refresh token so it can be revoked
        refreshTokenService.createRefreshToken(savedUser, refreshToken);

        log.info("User registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return buildAuthResponse(savedUser, accessToken, refreshToken, "Registration successful");
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().toLowerCase().trim(),
                request.getPassword()
            )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        // ── PHASE 7 ADDITION: Generate real JWT tokens ────────────
        String accessToken  = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        refreshTokenService.createRefreshToken(user, refreshToken);

        log.info("Login successful: id={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken, "Login successful");
    }

    
    @Transactional
    public AuthResponseDTO refreshAccessToken(RefreshTokenRequestDTO request) {
        log.debug("Refresh token request received");

        // Step 1: Validate
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(
            request.getRefreshToken()
        );

        User user = storedToken.getUser();
        UserPrincipal principal = UserPrincipal.create(user);

        // Step 2: Generate new access token
        String newAccessToken = jwtService.generateAccessToken(principal);

        // Step 3: Rotate refresh token (generate new one, revoke old)
        String newRefreshToken = jwtService.generateRefreshToken(principal);
        refreshTokenService.createRefreshToken(user, newRefreshToken);
        // createRefreshToken() revokes old tokens before saving new one

        log.debug("Access token refreshed for user: {}", user.getEmail());
        return buildAuthResponse(user, newAccessToken, newRefreshToken, "Token refreshed");
    }

    
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("User logged out, tokens revoked: userId={}", userId);
    }

    private AuthResponseDTO buildAuthResponse(User user, String accessToken,
                                               String refreshToken, String message) {
        Set<String> roleNames = user.getRoles()
            .stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());

        return AuthResponseDTO.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .roles(roleNames)
            .message(message)
            .build();
    }
}