package com.rentalhub.service;

import com.rentalhub.dto.request.ForgotPasswordRequestDTO;
import com.rentalhub.dto.request.ResetPasswordRequestDTO;
import com.rentalhub.entity.User;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService    emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // Token expires after 1 hour
    private static final int TOKEN_EXPIRY_HOURS = 1;

    /**
     * FORGOT PASSWORD — step 1.
     *
     * Generates a UUID reset token, stores it on the user,
     * and sends a reset email.
     *
     * SECURITY NOTE:
     * We always return the same success message whether the email
     * exists or not — this prevents user enumeration attacks
     * (attacker can't tell if an email is registered by checking
     * whether they get an error).
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        String email = request.getEmail().toLowerCase().trim();

        userRepository.findByEmail(email).ifPresent(user -> {
            // Generate UUID token — random, unguessable
            String token = UUID.randomUUID().toString();

            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(
                LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS)
            );
            userRepository.save(user);

            // Build reset link
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            // Send email asynchronously
            emailService.sendTemplatedEmail(
                user.getEmail(),
                "Reset Your RentalHub Password",
                "password-reset",
                Map.of(
                    "fullName",  user.getFullName(),
                    "resetLink", resetLink,
                    "expiryHours", TOKEN_EXPIRY_HOURS
                )
            );

            log.info("Password reset token generated for user: {}", email);
        });

        // Always log as if it succeeded — don't reveal if email exists
        log.info("Password reset requested for email: {}", email);
    }

    /**
     * RESET PASSWORD — step 2.
     *
     * Validates the token, updates the password, clears the token.
     *
     * Checks:
     *   1. Token exists in the database
     *   2. Token has not expired (within 1 hour)
     *   3. New password meets complexity requirements (done by @Valid)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository
            .findByPasswordResetToken(request.getToken())
            .orElseThrow(() -> new BadRequestException(
                "Invalid or expired reset token. " +
                "Please request a new password reset."
            ));

        // Check token expiry
        if (user.getPasswordResetTokenExpiry() == null
                || LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            // Clear expired token
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);

            throw new BadRequestException(
                "Reset token has expired. " +
                "Please request a new password reset."
            );
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Clear the token — single use only
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);

        // Revoke all active sessions/refresh tokens
        // so old tokens can't be used after password change
        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    /**
     * VALIDATE TOKEN — called by Angular when the reset page loads
     * to check if the token is still valid before showing the form.
     */
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return userRepository.findByPasswordResetToken(token)
            .map(user -> user.getPasswordResetTokenExpiry() != null
                && LocalDateTime.now().isBefore(user.getPasswordResetTokenExpiry()))
            .orElse(false);
    }
}