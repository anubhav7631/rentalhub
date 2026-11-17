package com.rentalhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalhub.dto.response.AgreementResponseDTO;
import com.rentalhub.entity.Agreement;
import com.rentalhub.service.AgreementService;
import com.rentalhub.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

/**
 * AgreementController — /api/agreements
 *
 * PERMISSION MODEL:
 *   POST   /agreements           — ADMIN, OWNER
 *   GET    /agreements           — ADMIN, OWNER (scoped)
 *   GET    /agreements/{id}      — ADMIN, owner of agreement, tenant of agreement
 *   GET    /agreements/{id}/download — same as above
 *   DELETE /agreements/{id}      — ADMIN or owner
 *   GET    /agreements/property/{id} — ADMIN or owner of that property
 *   POST   /agreements/admin/check-expiry — ADMIN only (manual trigger)
 */
@RestController
@RequestMapping("/agreements")
@Slf4j
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService   agreementService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper       objectMapper;

    
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<AgreementResponseDTO> upload(
            @RequestPart("metadata") String metadataJson,
            @RequestPart("file") MultipartFile file) throws IOException {

        log.info("POST /agreements — uploading agreement");
        AgreementResponseDTO result = agreementService.upload(metadataJson, file, objectMapper);
        return ResponseEntity.status(201).body(result);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping
    public ResponseEntity<Page<AgreementResponseDTO>> list(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(agreementService.list(pageable));
    }

    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@agreementSecurity.isTenantOrOwner(#id, authentication)"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AgreementResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(agreementService.getById(id));
    }

    
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@agreementSecurity.isTenantOrOwner(#id, authentication)"
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Agreement agreement = agreementService.getEntityById(id);

        try {
            Path filePath = fileStorageService.resolveAbsolute(agreement.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error("Agreement file not found or unreadable: {}", agreement.getFilePath());
                return ResponseEntity.notFound().build();
            }

            // Use the original filename stored in the DB for the download prompt
            String downloadFilename = agreement.getFileName() != null
                ? agreement.getFileName()
                : agreement.getAgreementNumber() + ".pdf";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + downloadFilename + "\""
                )
                .body(resource);

        } catch (MalformedURLException e) {
            log.error("Malformed URL for agreement file: {}", agreement.getFilePath(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/agreements/property/{propertyId}
     * History of all agreements for a specific property.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@propertySecurity.isOwner(#propertyId, authentication)"
    )
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<AgreementResponseDTO>> getByProperty(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(agreementService.getHistoryByProperty(propertyId));
    }

    
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@propertySecurity.isOwner(#propertyId, authentication)"
    )
    @GetMapping("/property/{propertyId}/active")
    public ResponseEntity<AgreementResponseDTO> getActiveByProperty(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(agreementService.getActiveByProperty(propertyId));
    }

    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
        "@agreementSecurity.isOwner(#id, authentication)"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agreementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/check-expiry")
    public ResponseEntity<String> triggerExpiryCheck() {
        agreementService.triggerExpiryCheck();
        return ResponseEntity.ok("Expiry check completed. Check server logs for details.");
    }
}