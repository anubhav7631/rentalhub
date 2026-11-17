package com.rentalhub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalhub.dto.request.MaintenanceRequestCreateDTO;
import com.rentalhub.dto.request.MaintenanceStatusUpdateDTO;
import com.rentalhub.dto.response.MaintenanceResponseDTO;
import com.rentalhub.entity.MaintenanceImage;
import com.rentalhub.entity.MaintenanceRequest;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.Tenant;
import com.rentalhub.entity.User;
import com.rentalhub.entity.enums.MaintenanceStatus;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.MaintenanceRequestRepository;
import com.rentalhub.repository.PropertyRepository;
import com.rentalhub.repository.TenantRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final PropertyRepository           propertyRepository;
    private final TenantRepository             tenantRepository;
    private final UserRepository               userRepository;
    private final FileStorageService           fileStorageService;
    private final ApplicationEventPublisher    eventPublisher;

    private static final Map<MaintenanceStatus, Set<MaintenanceStatus>> ALLOWED_TRANSITIONS =
        new EnumMap<>(MaintenanceStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
            MaintenanceStatus.OPEN,
            EnumSet.of(MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.REJECTED)
        );
        ALLOWED_TRANSITIONS.put(
            MaintenanceStatus.IN_PROGRESS,
            EnumSet.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.REJECTED)
        );
    }

    @Transactional
    public MaintenanceResponseDTO create(
            String metadataJson,
            List<MultipartFile> images,
            ObjectMapper objectMapper) {

        MaintenanceRequestCreateDTO dto;
        try {
            dto = objectMapper.readValue(metadataJson, MaintenanceRequestCreateDTO.class);
        } catch (Exception e) {
            throw new BadRequestException("Invalid request metadata: " + e.getMessage());
        }

        Property property = propertyRepository.findById(dto.getPropertyId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Property", dto.getPropertyId()));

        Tenant tenant = resolveTenant(dto.getPropertyId());

        MaintenanceRequest request = MaintenanceRequest.builder()
            .title(dto.getTitle())
            .description(dto.getDescription())
            .category(dto.getCategory())
            .status(MaintenanceStatus.OPEN)
            .property(property)
            .tenant(tenant)
            .build();

        MaintenanceRequest saved = maintenanceRequestRepository.save(request);

        if (images != null && !images.isEmpty()) {
            attachImages(saved, images);
            saved = maintenanceRequestRepository.save(saved);
        }

        log.info("Maintenance request created: id={}, property={}, title={}",
            saved.getId(), property.getId(), dto.getTitle());
        return toResponseDTO(saved);
    }

    @Transactional
    public MaintenanceResponseDTO updateStatus(
            Long requestId,
            MaintenanceStatusUpdateDTO dto) {

        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
            .orElseThrow(() ->
                new ResourceNotFoundException("MaintenanceRequest", requestId));

        MaintenanceStatus previousStatus = request.getStatus();
        validateTransition(request.getStatus(), dto.getStatus());

        request.setStatus(dto.getStatus());

        if (dto.getResolutionNotes() != null) {
            request.setResolutionNotes(dto.getResolutionNotes());
        }

        if (dto.getStatus() == MaintenanceStatus.COMPLETED
                || dto.getStatus() == MaintenanceStatus.REJECTED) {
            request.setResolvedAt(LocalDateTime.now());
        }

        if (dto.getAssignedToUserId() != null) {
            User assignee = userRepository.findById(dto.getAssignedToUserId())
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", dto.getAssignedToUserId()));
            request.setAssignedTo(assignee);
        }

        MaintenanceRequest savedRequest = maintenanceRequestRepository.save(request);

        // Publish event for notifications (Phase 14)
        eventPublisher.publishEvent(
            new com.rentalhub.event.MaintenanceStatusChangedEvent(
                this, savedRequest, previousStatus, dto.getStatus())
        );

        log.info("Maintenance request {} status: {} → {}",
            requestId, previousStatus, dto.getStatus());
        return toResponseDTO(savedRequest);
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceResponseDTO> list(
            MaintenanceStatus statusFilter,
            Pageable pageable) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin  = SecurityUtils.hasAuthority("ROLE_ADMIN");
        boolean isOwner  = SecurityUtils.hasAuthority("ROLE_OWNER");

        Page<MaintenanceRequest> results;

        if (isAdmin) {
            results = maintenanceRequestRepository.findAll(pageable);

        } else if (isOwner) {
            results = (statusFilter != null)
                ? maintenanceRequestRepository
                    .findByPropertyOwnerIdAndStatusOrderByCreatedAtDesc(
                        currentUserId, statusFilter, pageable)
                : maintenanceRequestRepository
                    .findByPropertyOwnerIdOrderByCreatedAtDesc(
                        currentUserId, pageable);

        } else {
            /*
             * TENANT branch — use findFirst to avoid
             * IncorrectResultSizeDataAccessException when
             * duplicate active tenant records exist.
             */
            Tenant tenantRecord = tenantRepository
                .findFirstByTenantUserIdAndMoveOutDateIsNullOrderByMoveInDateDesc(
                    currentUserId)
                .orElseThrow(() -> new BadRequestException(
                    "No active tenancy found for your account. " +
                    "Please contact your property owner."));

            results = maintenanceRequestRepository
                .findByTenantIdOrderByCreatedAtDesc(
                    tenantRecord.getId(), pageable);
        }

        return results.map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public MaintenanceResponseDTO getById(Long id) {
        return toResponseDTO(
            maintenanceRequestRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("MaintenanceRequest", id))
        );
    }

    @Transactional
    public MaintenanceResponseDTO addImages(Long requestId, List<MultipartFile> images) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
            .orElseThrow(() ->
                new ResourceNotFoundException("MaintenanceRequest", requestId));

        if (request.getStatus() != MaintenanceStatus.OPEN
                && request.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new BadRequestException(
                "Cannot add images to a " + request.getStatus() + " request");
        }

        attachImages(request, images);
        return toResponseDTO(maintenanceRequestRepository.save(request));
    }

    // ── Private helpers ──────────────────────────────────────────

    private void validateTransition(
            MaintenanceStatus current,
            MaintenanceStatus next) {

        Set<MaintenanceStatus> allowed = ALLOWED_TRANSITIONS.get(current);

        if (allowed == null || !allowed.contains(next)) {
            throw new BadRequestException(
                "Invalid status transition: " + current + " → " + next + ". " +
                "Allowed from " + current + ": " +
                (allowed != null ? allowed.toString() : "none (terminal state)")
            );
        }
    }

    private Tenant resolveTenant(Long propertyId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isTenant   = SecurityUtils.hasAuthority("ROLE_TENANT");

        if (isTenant) {
            /*
             * Use findFirst — prevents crash if duplicate active
             * tenant records exist for this user.
             */
            return tenantRepository
                .findFirstByTenantUserIdAndMoveOutDateIsNullOrderByMoveInDateDesc(
                    currentUserId)
                .orElseThrow(() -> new BadRequestException(
                    "You do not have an active tenancy. " +
                    "You must be an active tenant to raise a maintenance request."
                ));
        }

        return tenantRepository
            .findCurrentTenantByPropertyId(propertyId)
            .orElseThrow(() -> new BadRequestException(
                "No current tenant found for this property. " +
                "Assign a tenant before creating a maintenance request."
            ));
    }

    private void attachImages(
            MaintenanceRequest request,
            List<MultipartFile> images) {

        String subDir = "maintenance/" + request.getId();

        for (MultipartFile file : images) {
            if (file.isEmpty()) continue;

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Skipping non-image file: {}", file.getOriginalFilename());
                continue;
            }

            String path = fileStorageService.store(file, subDir);

            MaintenanceImage image = MaintenanceImage.builder()
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .contentType(contentType)
                .maintenanceRequest(request)
                .build();

            request.getImages().add(image);
        }
    }

    private MaintenanceResponseDTO toResponseDTO(MaintenanceRequest r) {
        List<String> imagePaths = r.getImages().stream()
            .map(MaintenanceImage::getFilePath)
            .collect(Collectors.toList());

        return MaintenanceResponseDTO.builder()
            .id(r.getId())
            .title(r.getTitle())
            .description(r.getDescription())
            .category(r.getCategory())
            .status(r.getStatus())
            .resolutionNotes(r.getResolutionNotes())
            .resolvedAt(r.getResolvedAt())
            .propertyId(r.getProperty().getId())
            .propertyName(r.getProperty().getPropertyName())
            .propertyAddress(
                r.getProperty().getAddressLine1() + ", " +
                r.getProperty().getCity()
            )
            .tenantId(r.getTenant().getId())
            .tenantName(r.getTenant().getFullName())
            .assignedToId(
                r.getAssignedTo() != null ? r.getAssignedTo().getId() : null)
            .assignedToName(
                r.getAssignedTo() != null ? r.getAssignedTo().getFullName() : null)
            .imagePaths(imagePaths)
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .build();
    }
}