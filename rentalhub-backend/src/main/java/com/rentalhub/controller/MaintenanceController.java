package com.rentalhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalhub.dto.request.MaintenanceStatusUpdateDTO;
import com.rentalhub.dto.response.MaintenanceResponseDTO;
import com.rentalhub.entity.enums.MaintenanceStatus;
import com.rentalhub.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * MaintenanceController — /api/maintenance
 *
 * PERMISSION MODEL:
 *   POST   /maintenance           — TENANT, OWNER, ADMIN
 *   GET    /maintenance           — ALL authenticated (scoped by role in service)
 *   GET    /maintenance/{id}      — owner of the property OR tenant who raised it
 *   PATCH  /maintenance/{id}/status — OWNER or ADMIN only
 *   POST   /maintenance/{id}/images — owner or tenant of the request
 */
@RestController
@RequestMapping("/maintenance")
@Slf4j
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final ObjectMapper       objectMapper;

    /**
     * POST /api/maintenance
     *
     * Multipart:
     *   "metadata" → JSON (MaintenanceRequestCreateDTO)
     *   "images"   → 0..N image files (optional)
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER', 'ROLE_TENANT')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<MaintenanceResponseDTO> create(
            @RequestPart("metadata") String metadataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
            throws IOException {

        log.info("POST /maintenance");
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(maintenanceService.create(metadataJson, images, objectMapper));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER', 'ROLE_TENANT')")
    @GetMapping
    public ResponseEntity<Page<MaintenanceResponseDTO>> list(
            @RequestParam(required = false) MaintenanceStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.list(status, pageable));
    }

    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@maintenanceSecurity.isOwnerOrTenant(#id, authentication)"
    )
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getById(id));
    }

    /**
     * PATCH /api/maintenance/{id}/status
     * Only owner/admin can change the status.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@maintenanceSecurity.isOwner(#id, authentication)"
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<MaintenanceResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceStatusUpdateDTO dto) {
        log.info("PATCH /maintenance/{}/status → {}", id, dto.getStatus());
        return ResponseEntity.ok(maintenanceService.updateStatus(id, dto));
    }

    /**
     * POST /api/maintenance/{id}/images
     * Add more images to an existing open request.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@maintenanceSecurity.isOwnerOrTenant(#id, authentication)"
    )
    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<MaintenanceResponseDTO> addImages(
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images) {
        return ResponseEntity.ok(maintenanceService.addImages(id, images));
    }
}