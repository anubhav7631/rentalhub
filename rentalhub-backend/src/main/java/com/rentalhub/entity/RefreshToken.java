package com.rentalhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_value", columnList = "token"),
    @Index(name = "idx_refresh_token_user",  columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

   
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

   
    @Column(name = "is_revoked", nullable = false,
            columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isRevoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Convenience ──────────────────────────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    public boolean isValid() {
        return !isRevoked && !isExpired();
    }
}