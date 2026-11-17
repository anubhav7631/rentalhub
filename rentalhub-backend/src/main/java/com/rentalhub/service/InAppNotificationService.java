package com.rentalhub.service;

import com.rentalhub.dto.response.NotificationResponseDTO;
import com.rentalhub.entity.Notification;
import com.rentalhub.entity.User;
import com.rentalhub.entity.enums.NotificationType;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.NotificationRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * InAppNotificationService — manages persisted in-app notifications.
 *
 * Separating in-app from email keeps each service small and focused.
 * In-app notifications are stored in the `notifications` table.
 * Email notifications are fire-and-forget (EmailService).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    /**
     * Create and persist a notification for a specific user.
     * Called by NotificationEventListener when events fire.
     */
    @Transactional
    public void create(
            Long recipientUserId,
            String title,
            String message,
            NotificationType type,
            Long referenceId,
            String referenceType) {

        User recipient = userRepository.findById(recipientUserId)
            .orElseGet(() -> {
                log.warn("Notification recipient not found: userId={}", recipientUserId);
                return null;
            });

        if (recipient == null) return;

        Notification notification = Notification.builder()
            .title(title)
            .message(message)
            .type(type)
            .isRead(false)
            .referenceId(referenceId)
            .referenceType(referenceType)
            .recipient(recipient)
            .build();

        notificationRepository.save(notification);
        log.debug("In-app notification created for user {}: {}", recipientUserId, title);
    }

    /**
     * GET unread count — called every 30 seconds from Angular topbar.
     * Uses the indexed `is_read` + `recipient_id` query for performance.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    /**
     * LIST notifications for the current user — paginated, newest first.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> listForCurrentUser(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository
            .findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toResponseDTO);
    }

    /**
     * MARK ONE as read.
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    /**
     * MARK ALL as read for current user.
     * Uses a single bulk UPDATE query (from Phase 5 repository).
     */
    @Transactional
    public void markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.markAllAsReadForUser(userId);
        log.debug("Marked all notifications as read for user {}", userId);
    }

    // ── Private ──────────────────────────────────────────────────
    private NotificationResponseDTO toResponseDTO(Notification n) {
        return NotificationResponseDTO.builder()
            .id(n.getId())
            .title(n.getTitle())
            .message(n.getMessage())
            .type(n.getType())
            .isRead(n.getIsRead())
            .referenceId(n.getReferenceId())
            .referenceType(n.getReferenceType())
            .createdAt(n.getCreatedAt())
            .build();
    }
}