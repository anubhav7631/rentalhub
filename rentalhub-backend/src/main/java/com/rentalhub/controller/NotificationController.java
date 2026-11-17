package com.rentalhub.controller;

import com.rentalhub.dto.response.NotificationResponseDTO;
import com.rentalhub.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService notificationService;

    /**
     * GET /api/notifications/unread-count
     * Called every 30 seconds by Angular topbar polling.
     * Returns a simple number — lightweight endpoint.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.rentalhub.security.UserPrincipal principal) {
        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /api/notifications
     * Paginated list of notifications for the current user.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listForCurrentUser(pageable));
    }

    /**
     * PATCH /api/notifications/{id}/read
     * Mark a single notification as read.
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/notifications/read-all
     * Mark all unread notifications as read for the current user.
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}