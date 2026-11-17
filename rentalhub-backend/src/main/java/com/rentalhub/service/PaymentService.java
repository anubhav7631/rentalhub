package com.rentalhub.service;

import com.rentalhub.dto.request.PaymentRequestDTO;
import com.rentalhub.dto.request.RecordPaymentRequestDTO;
import com.rentalhub.dto.response.PaymentResponseDTO;
import com.rentalhub.dto.response.PaymentSummaryDTO;
import com.rentalhub.entity.Payment;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.Tenant;
import com.rentalhub.entity.User;
import com.rentalhub.entity.enums.PaymentStatus;
import com.rentalhub.entity.enums.PropertyStatus;
import com.rentalhub.event.PaymentPaidEvent;
import com.rentalhub.event.RentDueEvent;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.exception.ResourceNotFoundException;
import com.rentalhub.repository.PaymentRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository     paymentRepository;
    private final PropertyRepository    propertyRepository;
    private final TenantRepository      tenantRepository;
    private final UserRepository        userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int DUE_DAY = 5;

    // ════════════════════════════════════════════════════════════════
    // CREATE MANUALLY
    // ════════════════════════════════════════════════════════════════

    /**
     * Owner creates a payment record manually for a specific month.
     * Guards against duplicates before inserting.
     */
    @Transactional
    public PaymentResponseDTO createManually(PaymentRequestDTO request) {
        Property property = propertyRepository.findById(request.getPropertyId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Property", request.getPropertyId()));

        Tenant tenant = tenantRepository.findById(request.getTenantId())
            .orElseThrow(() ->
                new ResourceNotFoundException("Tenant", request.getTenantId()));

        if (paymentRepository.existsByPropertyIdAndTenantIdAndRentMonthAndRentYear(
                property.getId(), tenant.getId(),
                request.getRentMonth(), request.getRentYear())) {
            throw new BadRequestException(
                "A payment record already exists for " +
                monthLabel(request.getRentMonth(), request.getRentYear())
            );
        }

        Payment payment = Payment.builder()
            .amount(request.getAmount())
            .rentMonth(request.getRentMonth())
            .rentYear(request.getRentYear())
            .dueDate(request.getDueDate())
            .status(PaymentStatus.PENDING)
            .notes(request.getNotes())
            .property(property)
            .tenant(tenant)
            .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created manually: property={}, tenant={}, month={}/{}",
            property.getId(), tenant.getId(),
            request.getRentMonth(), request.getRentYear());
        return toResponseDTO(saved);
    }

    // ════════════════════════════════════════════════════════════════
    // RECORD PAYMENT (MARK AS PAID)
    // ════════════════════════════════════════════════════════════════

    /**
     * Owner marks a PENDING or OVERDUE payment as PAID.
     * Publishes PaymentPaidEvent for notifications (Phase 14).
     */
    @Transactional
    public PaymentResponseDTO recordPayment(
            Long paymentId,
            RecordPaymentRequestDTO request) {

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.isPaid()) {
            throw new BadRequestException(
                "This payment is already recorded as PAID");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        User recorder = userRepository.findById(currentUserId)
            .orElseThrow(() ->
                new ResourceNotFoundException("User", currentUserId));

        // Allow optional amount override
        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(request.getPaidDate());
        payment.setPaymentReference(request.getPaymentReference());
        payment.setRecordedBy(recorder);

        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }

        Payment saved = paymentRepository.save(payment);

        // Publish event → NotificationEventListener sends email + in-app notification
        eventPublisher.publishEvent(new PaymentPaidEvent(this, saved));

        log.info("Payment {} marked PAID by user {}", paymentId, currentUserId);
        return toResponseDTO(saved);
    }

    // ════════════════════════════════════════════════════════════════
    // WAIVE PAYMENT
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public PaymentResponseDTO waivePayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.isPaid()) {
            throw new BadRequestException("Cannot waive an already-paid payment");
        }

        payment.setStatus(PaymentStatus.WAIVED);
        payment.setNotes(reason);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} waived", paymentId);
        return toResponseDTO(saved);
    }

    // ════════════════════════════════════════════════════════════════
    // LIST PAYMENTS — role-aware
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns payments scoped by the caller's role:
     *   ADMIN  → all payments (or filtered by propertyId/tenantId)
     *   OWNER  → payments for their own properties
     *   TENANT → their own payments only
     *
     * Uses findFirst for tenant lookup to avoid crash
     * on duplicate active tenant records.
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> list(
            Long propertyId,
            Long tenantId,
            Pageable pageable) {

        boolean isAdmin  = SecurityUtils.hasAuthority("ROLE_ADMIN");
        boolean isTenant = SecurityUtils.hasAuthority("ROLE_TENANT");
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Page<Payment> payments;

        if (propertyId != null) {
            // Filter by specific property
            payments = paymentRepository
                .findByPropertyIdOrderByRentYearDescRentMonthDesc(
                    propertyId, pageable);

        } else if (tenantId != null) {
            // Filter by specific tenant
            payments = paymentRepository
                .findByTenantIdOrderByRentYearDescRentMonthDesc(
                    tenantId, pageable);

        } else if (isAdmin) {
            // Admin sees everything
            payments = paymentRepository.findAll(pageable);

        } else if (isTenant) {
            /*
             * TENANT branch.
             *
             * Uses findFirst to safely handle cases where the tenant
             * user has multiple active tenant records (data inconsistency).
             * Returns Page.empty() instead of throwing if no active
             * tenancy is found — the tenant just sees an empty list.
             */
            return tenantRepository
                .findFirstByTenantUserIdAndMoveOutDateIsNullOrderByMoveInDateDesc(
                    currentUserId)
                .map(tenant ->
                    paymentRepository
                        .findByTenantIdOrderByRentYearDescRentMonthDesc(
                            tenant.getId(), pageable)
                        .map(this::toResponseDTO))
                .orElse(Page.empty(pageable));

        } else {
            // OWNER — see payments for all their properties
            payments = paymentRepository
                .findByPropertyOwnerIdOrderByRentYearDescRentMonthDesc(
                    currentUserId, pageable);
        }

        return payments.map(this::toResponseDTO);
    }

    // ════════════════════════════════════════════════════════════════
    // GET BY ID
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PaymentResponseDTO getById(Long id) {
        return toResponseDTO(
            paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id))
        );
    }

    // ════════════════════════════════════════════════════════════════
    // SUMMARY — aggregated stats for the dashboard cards
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PaymentSummaryDTO getSummary() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LocalDate now  = LocalDate.now();
        int month      = now.getMonthValue();
        int year       = now.getYear();

        BigDecimal collected = paymentRepository
            .sumPaidByOwnerAndMonth(currentUserId, month, year);

        BigDecimal pending = paymentRepository
            .sumPendingByOwner(currentUserId);

        // Sum all OVERDUE amounts for this owner
        BigDecimal overdueTotal = paymentRepository
            .findByPropertyOwnerIdAndStatus(currentUserId, PaymentStatus.OVERDUE)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount    = paymentRepository
            .countByPropertyOwnerIdAndStatus(currentUserId, PaymentStatus.PAID);
        long pendingCount = paymentRepository
            .countByPropertyOwnerIdAndStatus(currentUserId, PaymentStatus.PENDING);
        long overdueCount = paymentRepository
            .countByPropertyOwnerIdAndStatus(currentUserId, PaymentStatus.OVERDUE);

        return PaymentSummaryDTO.builder()
            .collectedThisMonth(collected != null ? collected : BigDecimal.ZERO)
            .pendingThisMonth(pending    != null ? pending   : BigDecimal.ZERO)
            .overdueTotal(overdueTotal)
            .paidCount(paidCount)
            .pendingCount(pendingCount)
            .overdueCount(overdueCount)
            .currentMonth(month)
            .currentYear(year)
            .build();
    }

    // ════════════════════════════════════════════════════════════════
    // GENERATE PAYMENTS FOR A MONTH
    // ════════════════════════════════════════════════════════════════

    /**
     * Creates PENDING payment records for every occupied property.
     * Idempotent — skips if a record already exists for that month.
     * Called by the @Scheduled job and manually from the controller.
     */
    @Transactional
    public int generatePaymentsForMonth(int month, int year) {
        List<Property> occupiedProperties = propertyRepository
            .findAll()
            .stream()
            .filter(p -> p.getStatus() == PropertyStatus.OCCUPIED)
            .toList();

        int[] created = {0};

        for (Property property : occupiedProperties) {
            tenantRepository
                .findCurrentTenantByPropertyId(property.getId())
                .ifPresent(tenant -> {
                    boolean exists = paymentRepository
                        .existsByPropertyIdAndTenantIdAndRentMonthAndRentYear(
                            property.getId(), tenant.getId(), month, year);

                    if (!exists) {
                        LocalDate dueDate = LocalDate.of(year, month, DUE_DAY);

                        Payment payment = Payment.builder()
                            .amount(property.getRentAmount())
                            .rentMonth(month)
                            .rentYear(year)
                            .dueDate(dueDate)
                            .status(PaymentStatus.PENDING)
                            .property(property)
                            .tenant(tenant)
                            .build();

                        paymentRepository.save(payment);
                        created[0]++;

                        log.debug("Generated payment: property={}, tenant={}, {}/{}",
                            property.getId(), tenant.getId(), month, year);
                    }
                });
        }

        log.info("Payment generation for {}/{}: {} new records created",
            month, year, created[0]);
        return created[0];
    }

    // ════════════════════════════════════════════════════════════════
    // SCHEDULED JOBS
    // ════════════════════════════════════════════════════════════════

    /**
     * AUTO-GENERATE — runs on the 1st of every month at 6:00 AM.
     */
    @Scheduled(cron = "0 0 6 1 * *")
    @Transactional
    public void autoGenerateMonthlyPayments() {
        LocalDate today = LocalDate.now();
        log.info("Auto-generating payments for {}/{}",
            today.getMonthValue(), today.getYear());
        generatePaymentsForMonth(today.getMonthValue(), today.getYear());
    }

    /**
     * MARK OVERDUE — runs daily at 7:00 AM.
     * Finds all PENDING payments past their due date and marks them OVERDUE.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void markOverduePayments() {
        LocalDate today = LocalDate.now();
        List<Payment> overdue = paymentRepository.findPendingOverdue(today);

        if (overdue.isEmpty()) {
            log.debug("No overdue payments found for {}", today);
            return;
        }

        overdue.forEach(p -> {
            p.setStatus(PaymentStatus.OVERDUE);
            log.info("Payment {} marked OVERDUE (dueDate={}, property={}, tenant={})",
                p.getId(), p.getDueDate(),
                p.getProperty().getId(),
                p.getTenant().getFullName());
        });

        paymentRepository.saveAll(overdue);
        log.info("Marked {} payment(s) as OVERDUE", overdue.size());
    }

    /**
     * RENT DUE REMINDERS — runs daily at 9:00 AM.
     * Publishes RentDueEvent for payments due in exactly 3 days.
     * NotificationEventListener (Phase 14) handles email + in-app notifications.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void publishRentDueReminders() {
        LocalDate in3Days = LocalDate.now().plusDays(3);

        paymentRepository.findAll().stream()
            .filter(p -> p.getStatus() == PaymentStatus.PENDING
                && p.getDueDate().equals(in3Days))
            .forEach(p ->
                eventPublisher.publishEvent(new RentDueEvent(this, p, 3))
            );
    }

    // ════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════

    private String monthLabel(int month, int year) {
        return Month.of(month)
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }

    private PaymentResponseDTO toResponseDTO(Payment p) {
        return PaymentResponseDTO.builder()
            .id(p.getId())
            .amount(p.getAmount())
            .rentMonth(p.getRentMonth())
            .rentYear(p.getRentYear())
            .monthLabel(monthLabel(p.getRentMonth(), p.getRentYear()))
            .dueDate(p.getDueDate())
            .paidDate(p.getPaidDate())
            .status(p.getStatus())
            .lateFee(p.getLateFee())
            .paymentReference(p.getPaymentReference())
            .notes(p.getNotes())
            .propertyId(p.getProperty().getId())
            .propertyName(p.getProperty().getPropertyName())
            .tenantId(p.getTenant().getId())
            .tenantName(p.getTenant().getFullName())
            .recordedByName(
                p.getRecordedBy() != null
                    ? p.getRecordedBy().getFullName()
                    : null)
            .createdAt(p.getCreatedAt())
            .build();
    }
}