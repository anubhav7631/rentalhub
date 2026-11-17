package com.rentalhub.controller;

import com.rentalhub.dto.request.ForgotPasswordRequestDTO;
import com.rentalhub.dto.request.LoginRequestDTO;
import com.rentalhub.dto.request.RefreshTokenRequestDTO;
import com.rentalhub.dto.request.ResetPasswordRequestDTO;
import com.rentalhub.dto.request.UserRequestDTO;
import com.rentalhub.dto.response.AuthResponseDTO;
import com.rentalhub.security.UserPrincipal;
import com.rentalhub.service.AuthService;
import com.rentalhub.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody UserRequestDTO request) {
        log.info("POST /auth/register - email: {}", request.getEmail());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {
        log.info("POST /auth/login - email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        log.debug("POST /auth/refresh");
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }
   
    /**
     * POST /api/auth/forgot-password
     * Accepts an email and sends a reset link.
     * Always returns 200 — never reveals if email exists.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
            "message",
            "If an account exists with this email, " +
            "a password reset link has been sent."
        ));
    }

    /**
     * POST /api/auth/reset-password
     * Validates the token and updates the password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
            "message", "Password reset successfully. You can now log in."
        ));
    }

    /**
     * GET /api/auth/validate-reset-token?token=xxx
     * Angular calls this when the reset page loads to check
     * if the token is still valid before showing the form.
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Boolean>> validateToken(
            @RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
            "userId",   principal.getId(),
            "email",    principal.getUsername(),
            "fullName", principal.getUser().getFullName(),
            "roles",    principal.getAuthorities()
                            .stream()
                            .map(a -> a.getAuthority())
                            .toList()
        ));
    }
}