package com.rentalhub.service;

import com.rentalhub.dto.request.MoveOutRequestDTO;
import com.rentalhub.dto.request.TenantRequestDTO;
import com.rentalhub.dto.response.TenantResponseDTO;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.Tenant;
import com.rentalhub.entity.User;
import com.rentalhub.entity.enums.PropertyStatus;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.PropertyRepository;
import com.rentalhub.repository.TenantRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public TenantResponseDTO assignTenant(TenantRequestDTO request) {

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", request.getPropertyId()));

        if (tenantRepository.existsByPropertyIdAndMoveOutDateIsNull(property.getId())) {
            throw new BadRequestException(
                    "This property already has a current tenant. " +
                    "End the existing tenancy before assigning a new one."
            );
        }

        if (property.getStatus() == PropertyStatus.INACTIVE) {
            throw new BadRequestException(
                    "Cannot assign a tenant to an inactive property");
        }

        /*
         * Try to find a registered user with this email (case-insensitive).
         * If found → link the Tenant record to their User account so they
         * can log in and see their tenancy on the dashboard.
         *
         * If NOT found → tenant_user_id stays null. The owner can still
         * manage the tenant. If the tenant registers later, call
         * linkTenantToUser() to fix the link.
         */

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User tenantUser = userRepository.findByEmail(normalizedEmail).orElse(null);

        log.info("Assigning tenant with email={}, matched user={}",
                normalizedEmail,
                tenantUser != null
                        ? tenantUser.getId()
                        : "none — tenant_user_id will be null");

        Tenant tenant = Tenant.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .phone(request.getPhone())
                .idProofNumber(request.getIdProofNumber())
                .idProofType(request.getIdProofType())
                .moveInDate(request.getMoveInDate())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .notes(request.getNotes())
                .property(property)
                .tenantUser(tenantUser)
                .build();

        Tenant saved = tenantRepository.save(tenant);

        property.setStatus(PropertyStatus.OCCUPIED);
        propertyRepository.save(property);

        log.info("Tenant assigned: tenantId={}, propertyId={}, linked={}",
                saved.getId(), property.getId(), tenantUser != null);

        return toResponseDTO(saved);
    }

    @Transactional
    public TenantResponseDTO moveOut(
            Long tenantId,
            MoveOutRequestDTO request) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Tenant", tenantId));

        if (!tenant.isCurrentTenant()) {
            throw new BadRequestException(
                    "This tenant has already moved out");
        }

        if (request.getMoveOutDate().isBefore(tenant.getMoveInDate())) {
            throw new BadRequestException(
                    "Move-out date cannot be before move-in date");
        }

        tenant.setMoveOutDate(request.getMoveOutDate());

        if (request.getNotes() != null) {
            tenant.setNotes(
                    (tenant.getNotes() != null
                            ? tenant.getNotes() + "\n"
                            : "") +
                    "Move-out note: " + request.getNotes()
            );
        }

        Tenant saved = tenantRepository.save(tenant);

        // ── Free up the property ───────────────────────────────────
        Property property = tenant.getProperty();

        property.setStatus(PropertyStatus.AVAILABLE);
        propertyRepository.save(property);

        log.info(
                "Tenant moved out: tenantId={}, propertyId={}",
                tenantId,
                property.getId()
        );

        return toResponseDTO(saved);
    }

    @Transactional
    public TenantResponseDTO updateProfile(
            Long tenantId,
            TenantRequestDTO request) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Tenant", tenantId));

        tenant.setFullName(request.getFullName());
        tenant.setEmail(request.getEmail().toLowerCase().trim());
        tenant.setPhone(request.getPhone());
        tenant.setIdProofNumber(request.getIdProofNumber());
        tenant.setIdProofType(request.getIdProofType());
        tenant.setEmergencyContactName(
                request.getEmergencyContactName()
        );
        tenant.setEmergencyContactPhone(
                request.getEmergencyContactPhone()
        );
        tenant.setNotes(request.getNotes());

        // Deliberately NOT updating moveInDate or propertyId here —
        // changing which property a tenant is on, or backdating their
        // move-in, are different operations with different implications.

        Tenant saved = tenantRepository.save(tenant);

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public TenantResponseDTO getById(Long id) {

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Tenant", id));

        return toResponseDTO(tenant);
    }

    @Transactional(readOnly = true)
    public Page<TenantResponseDTO> listCurrentTenants(
            Pageable pageable) {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        boolean isAdmin =
                SecurityUtils.hasAuthority("ROLE_ADMIN");

        Page<Tenant> tenants = isAdmin
                ? tenantRepository.findAll(pageable)
                : tenantRepository.findCurrentTenantsByOwner(
                        currentUserId, pageable);

        return tenants.map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<TenantResponseDTO> getHistoryByProperty(
            Long propertyId) {

        return tenantRepository
                .findByPropertyIdOrderByMoveInDateDesc(propertyId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TenantResponseDTO> getCurrentTenantForProperty(
            Long propertyId) {

        return tenantRepository
                .findCurrentTenantByPropertyId(propertyId)
                .map(this::toResponseDTO);
    }

    /**
     * linkTenantToUser — connects an existing Tenant record to a
     * registered User account by email.
     *
     * Call this when:
     * 1. The tenant was assigned before registering on RentalHub
     * 2. There was an email mismatch at assignment time
     */
    @Transactional
    public TenantResponseDTO linkTenantToUser(
            Long tenantId,
            String email) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tenant", tenantId));

        String normalizedEmail = email.toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException(
                        "No registered user found with email: " +
                        normalizedEmail +
                        ". Ask the tenant to register on RentalHub first."
                ));

        // Confirm user has ROLE_TENANT
        boolean isTenant = user.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() ==
                        com.rentalhub.entity.enums.RoleName.ROLE_TENANT
                );

        if (!isTenant) {
            throw new BadRequestException(
                    "User " + normalizedEmail +
                    " does not have ROLE_TENANT. " +
                    "They must register with the Tenant role."
            );
        }

        tenant.setTenantUser(user);

        Tenant saved = tenantRepository.save(tenant);

        log.info(
                "Linked tenant {} to user {} ({})",
                tenantId,
                user.getId(),
                normalizedEmail
        );

        return toResponseDTO(saved);
    }

    // ── Private mapper ──────────────────────────────────────────
    private TenantResponseDTO toResponseDTO(Tenant tenant) {

        Property property = tenant.getProperty();

        return TenantResponseDTO.builder()
                .id(tenant.getId())
                .fullName(tenant.getFullName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .idProofNumber(tenant.getIdProofNumber())
                .idProofType(tenant.getIdProofType())
                .moveInDate(tenant.getMoveInDate())
                .moveOutDate(tenant.getMoveOutDate())
                .isCurrentTenant(tenant.isCurrentTenant())
                .emergencyContactName(
                        tenant.getEmergencyContactName())
                .emergencyContactPhone(
                        tenant.getEmergencyContactPhone())
                .notes(tenant.getNotes())
                .propertyId(property.getId())
                .propertyName(property.getPropertyName())
                .propertyAddress(
                        property.getAddressLine1() +
                        ", " +
                        property.getCity()
                )
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}