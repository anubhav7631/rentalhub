package com.rentalhub.controller;

import com.rentalhub.entity.AuditLog;
import com.rentalhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AdminAuditController — admin-only audit log access.
 * Lets admins see who did what and when.
 */
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    /**
     * GET /api/admin/audit
     * All audit logs, newest first.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @PageableDefault(size = 20, sort = "performedAt") Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    /**
     * GET /api/admin/audit/entity/{type}/{id}
     * Audit history for a specific entity, e.g. /entity/Payment/5
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLog>> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
            auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
                entityType, entityId, pageable)
        );
    }

    /**
     * GET /api/admin/audit/user/{userId}
     * All actions performed by a specific user.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLog>> getByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
            auditLogRepository.findByPerformedByUserIdOrderByPerformedAtDesc(
                userId, pageable)
        );
    }
}