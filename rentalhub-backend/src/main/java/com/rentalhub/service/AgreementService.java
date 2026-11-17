package com.rentalhub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalhub.dto.request.AgreementRequestDTO;
import com.rentalhub.dto.response.AgreementResponseDTO;
import com.rentalhub.entity.Agreement;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.Tenant;
import com.rentalhub.entity.User;
import com.rentalhub.event.AgreementExpiryEvent;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.AgreementRepository;
import com.rentalhub.repository.PropertyRepository;
import com.rentalhub.repository.TenantRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final PropertyRepository  propertyRepository;
    private final TenantRepository    tenantRepository;
    private final UserRepository      userRepository;
    private final FileStorageService  fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    // How many days before expiry to flag as "expiring soon"
    private static final int EXPIRY_WARNING_DAYS = 30;


    @Transactional
    public AgreementResponseDTO upload(
            String metadataJson,
            MultipartFile file,
            ObjectMapper objectMapper) {

        // Parse metadata JSON → DTO
        AgreementRequestDTO request;
        try {
            request = objectMapper.readValue(metadataJson, AgreementRequestDTO.class);
        } catch (Exception e) {
            throw new BadRequestException("Invalid agreement metadata: " + e.getMessage());
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Agreement PDF file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BadRequestException("Only PDF files are accepted for agreements");
        }

        // Validate date logic
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        // Verify property and tenant exist and are linked
        Property property = propertyRepository.findById(request.getPropertyId())
            .orElseThrow(() -> new ResourceNotFoundException("Property", request.getPropertyId()));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.getTenantId()));

        if (!tenant.getProperty().getId().equals(property.getId())) {
            throw new BadRequestException(
                "Tenant is not assigned to this property. " +
                "Assign the tenant to the property before uploading an agreement."
            );
        }

        // Deactivate any existing active agreement for this property
        agreementRepository.findByPropertyIdAndIsActiveTrue(property.getId())
            .ifPresent(existing -> {
                existing.setIsActive(false);
                agreementRepository.save(existing);
                log.info("Deactivated previous agreement {} for property {}",
                    existing.getAgreementNumber(), property.getId());
            });

        // Get uploader
        Long uploaderId = SecurityUtils.getCurrentUserId();
        User uploader = userRepository.findById(uploaderId)
            .orElseThrow(() -> new ResourceNotFoundException("User", uploaderId));

        // Store file on disk
        String subDir   = "agreements/" + property.getId();
        String filePath = fileStorageService.store(file, subDir);

        // Generate agreement number: AGR-2024-0001
        String agreementNumber = generateAgreementNumber();

        Agreement agreement = Agreement.builder()
            .agreementNumber(agreementNumber)
            .filePath(filePath)
            .fileName(file.getOriginalFilename())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .monthlyRent(request.getMonthlyRent())
            .depositAmount(request.getDepositAmount())
            .isActive(true)
            .isExpired(false)
            .notes(request.getNotes())
            .property(property)
            .tenant(tenant)
            .uploadedBy(uploader)
            .build();

        Agreement saved = agreementRepository.save(agreement);
        log.info("Agreement {} uploaded for property {}, tenant {}",
            agreementNumber, property.getId(), tenant.getId());

        return toResponseDTO(saved);
    }

    /**
     * LIST agreements — role-aware.
     * ADMIN sees all. OWNER sees only their own.
     */
    @Transactional(readOnly = true)
    public Page<AgreementResponseDTO> list(Pageable pageable) {
        boolean isAdmin = SecurityUtils.hasAuthority("ROLE_ADMIN");
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Page<Agreement> agreements = isAdmin
            ? agreementRepository.findAll(pageable)
            : agreementRepository.findByOwnerIdOrderByStartDateDesc(currentUserId, pageable);

        return agreements.map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public AgreementResponseDTO getById(Long id) {
        Agreement agreement = agreementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agreement", id));
        return toResponseDTO(agreement);
    }

    /**
     * GET the active agreement for a property.
     * Used by tenant dashboard and property detail page.
     */
    @Transactional(readOnly = true)
    public AgreementResponseDTO getActiveByProperty(Long propertyId) {
        return agreementRepository.findByPropertyIdAndIsActiveTrue(propertyId)
            .map(this::toResponseDTO)
            .orElseThrow(() ->
                new ResourceNotFoundException("No active agreement found for property " + propertyId)
            );
    }

    /**
     * GET the file path for a specific agreement.
     * Used by the download endpoint in the controller.
     */
    @Transactional(readOnly = true)
    public Agreement getEntityById(Long id) {
        return agreementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agreement", id));
    }

    /**
     * DELETE (soft) — marks agreement inactive and deletes the file.
     */
    @Transactional
    public void delete(Long id) {
        Agreement agreement = agreementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agreement", id));

        // Delete physical file
        fileStorageService.delete(agreement.getFilePath());

        // Remove the DB record entirely (hard delete for agreements is acceptable —
        // the agreement number and financial terms remain in audit logs in Phase 17)
        agreementRepository.delete(agreement);
        log.info("Agreement {} deleted", agreement.getAgreementNumber());
    }

    /**
     * RENEW — creates a new agreement for the same property/tenant
     * with updated dates. The old one is deactivated automatically
     * inside upload() when it finds an existing active agreement.
     */
    @Transactional(readOnly = true)
    public List<AgreementResponseDTO> getHistoryByProperty(Long propertyId) {
        return agreementRepository
            .findByPropertyIdOrderByStartDateDesc(propertyId, Pageable.unpaged())
            .map(this::toResponseDTO)
            .toList();
    }


    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkExpiringAgreements() {
        LocalDate today       = LocalDate.now();
        LocalDate warningDate = today.plusDays(EXPIRY_WARNING_DAYS);

        // ── Flag EXPIRED agreements ────────────────────────────────
        List<Agreement> nowExpired = agreementRepository.findExpiredNotFlagged(today);

        if (!nowExpired.isEmpty()) {
            nowExpired.forEach(a -> {
                a.setIsExpired(true);
                a.setIsActive(false);

                log.info("Agreement {} (property {}) marked expired",
                    a.getAgreementNumber(), a.getProperty().getId());
            });

            agreementRepository.saveAll(nowExpired);

            log.info("Marked {} agreements as expired", nowExpired.size());
        }

        // ── EXPIRING SOON agreements ───────────────────────────────
        List<Agreement> expiringSoon =
            agreementRepository.findExpiringSoon(today, warningDate);

        if (!expiringSoon.isEmpty()) {

            expiringSoon.forEach(a -> {
                long days = ChronoUnit.DAYS.between(today, a.getEndDate());

                // Publish event — NotificationEventListener handles email + in-app
                eventPublisher.publishEvent(
                    new AgreementExpiryEvent(this, a, days)
                );

                log.info(
                    "Published AgreementExpiryEvent for agreement {} ({} days)",
                    a.getAgreementNumber(),
                    days
                );
            });

            log.info("{} agreement(s) expiring within {} days",
                expiringSoon.size(), EXPIRY_WARNING_DAYS);

        } else {
            log.debug("No agreements expiring within {} days",
                EXPIRY_WARNING_DAYS);
        }
    }


    @Transactional
    public void triggerExpiryCheck() {
        checkExpiringAgreements();
    }


    private String generateAgreementNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "AGR-" + year + "-";

        long count =
            agreementRepository.countByAgreementNumberStartingWith(prefix);

        return String.format("%s%04d", prefix, count + 1);
    }

    private String computeExpiryStatus(Agreement agreement) {
        if (agreement.getIsExpired()) return "EXPIRED";

        long days = ChronoUnit.DAYS.between(
            LocalDate.now(),
            agreement.getEndDate()
        );

        if (days < 0) return "EXPIRED";
        if (days <= EXPIRY_WARNING_DAYS) return "EXPIRING_SOON";

        return "ACTIVE";
    }

    private AgreementResponseDTO toResponseDTO(Agreement agreement) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(
            LocalDate.now(), agreement.getEndDate()
        );

        return AgreementResponseDTO.builder()
            .id(agreement.getId())
            .agreementNumber(agreement.getAgreementNumber())
            .filePath(agreement.getFilePath())
            .fileName(agreement.getFileName())
            .startDate(agreement.getStartDate())
            .endDate(agreement.getEndDate())
            .monthlyRent(agreement.getMonthlyRent())
            .depositAmount(agreement.getDepositAmount())
            .isActive(agreement.getIsActive())
            .isExpired(agreement.getIsExpired())
            .notes(agreement.getNotes())
            .propertyId(agreement.getProperty().getId())
            .propertyName(agreement.getProperty().getPropertyName())
            .propertyAddress(
                agreement.getProperty().getAddressLine1() + ", " +
                agreement.getProperty().getCity()
            )
            .tenantId(agreement.getTenant().getId())
            .tenantName(agreement.getTenant().getFullName())
            .uploadedByName(
                agreement.getUploadedBy() != null
                    ? agreement.getUploadedBy().getFullName()
                    : "System"
            )
            .daysUntilExpiry(daysUntilExpiry)
            .expiryStatus(computeExpiryStatus(agreement))
            .createdAt(agreement.getCreatedAt())
            .build();
    }
}