package com.rentalhub.controller;

import com.rentalhub.dto.request.MoveOutRequestDTO;
import com.rentalhub.dto.request.TenantRequestDTO;
import com.rentalhub.dto.response.TenantResponseDTO;
import com.rentalhub.service.TenantService;

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

import java.util.List;

@RestController
@RequestMapping("/tenants")
@Slf4j
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#request.propertyId, authentication)")
    @PostMapping
    public ResponseEntity<TenantResponseDTO> assignTenant(
            @Valid @RequestBody TenantRequestDTO request) {

        log.info(
                "Assigning tenant {} to property {}",
                request.getEmail(),
                request.getPropertyId()
        );

        TenantResponseDTO created = tenantService.assignTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping
    public ResponseEntity<Page<TenantResponseDTO>> listCurrentTenants(
            @PageableDefault(
                    size = 10,
                    sort = "moveInDate",
                    direction = org.springframework.data.domain.Sort.Direction.DESC
            )
            Pageable pageable) {

        return ResponseEntity.ok(
                tenantService.listCurrentTenants(pageable)
        );
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.isOwnerOfTenant(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDTO> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(tenantService.getById(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.isOwnerOfTenant(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponseDTO> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody TenantRequestDTO request) {

        return ResponseEntity.ok(
                tenantService.updateProfile(id, request)
        );
    }

    /**
     * POST /api/tenants/{id}/link-user
     * Links a Tenant record to a registered User account.
     * Used when:
     *   - Tenant was assigned before they registered
     *   - Email mismatch was later corrected
     *
     * Body: { "email": "tenant@example.com" }
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @PostMapping("/{id}/link-user")
    public ResponseEntity<TenantResponseDTO> linkUser(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {

        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        TenantResponseDTO result =
                tenantService.linkTenantToUser(id, email);

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.isOwnerOfTenant(#id, authentication)")
    @PostMapping("/{id}/move-out")
    public ResponseEntity<TenantResponseDTO> moveOut(
            @PathVariable Long id,
            @Valid @RequestBody MoveOutRequestDTO request) {

        log.info("Processing move-out for tenant {}", id);
        return ResponseEntity.ok(
                tenantService.moveOut(id, request)
        );
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#propertyId, authentication)")
    @GetMapping("/property/{propertyId}/history")
    public ResponseEntity<List<TenantResponseDTO>> getHistory(
            @PathVariable Long propertyId) {

        return ResponseEntity.ok(
                tenantService.getHistoryByProperty(propertyId)
        );
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @propertySecurity.isOwner(#propertyId, authentication)")
    @GetMapping("/property/{propertyId}/current")
    public ResponseEntity<TenantResponseDTO> getCurrentTenant(
            @PathVariable Long propertyId) {

        return tenantService.getCurrentTenantForProperty(propertyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}