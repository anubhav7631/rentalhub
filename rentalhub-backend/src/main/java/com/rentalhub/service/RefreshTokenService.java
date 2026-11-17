package com.rentalhub.service;

import com.rentalhub.entity.RefreshToken;
import com.rentalhub.entity.User;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    
    @Transactional
    public RefreshToken createRefreshToken(User user, String tokenValue) {
        // Revoke any existing tokens for this user before creating new
        // Enforces single-session policy: only one active refresh token per user
        // Remove this line to allow multi-device login
        refreshTokenRepository.revokeAllByUserId(user.getId());

        LocalDateTime expiry = LocalDateTime.now()
            .plusSeconds(refreshExpirationMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
            .token(tokenValue)
            .user(user)
            .expiryDate(expiry)
            .isRevoked(false)
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
            .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.getIsRevoked()) {
            log.warn("Attempt to use revoked refresh token for user: {}",
                refreshToken.getUser().getEmail());
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        return refreshToken;
    }

    
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked all refresh tokens for user id: {}", userId);
    }
}