package com.rentalhub.controller;

import com.rentalhub.dto.response.AdminDashboardDTO;
import com.rentalhub.dto.response.OwnerDashboardDTO;
import com.rentalhub.dto.response.TenantDashboardDTO;
import com.rentalhub.entity.Agreement;
import com.rentalhub.entity.Payment;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.Tenant;
import com.rentalhub.entity.enums.MaintenanceStatus;
import com.rentalhub.entity.enums.PaymentStatus;
import com.rentalhub.entity.enums.PropertyStatus;
import com.rentalhub.entity.enums.RoleName;
import com.rentalhub.exception.BadRequestException;
import com.rentalhub.repository.AgreementRepository;
import com.rentalhub.repository.MaintenanceRequestRepository;
import com.rentalhub.repository.PaymentRepository;
import com.rentalhub.repository.PropertyRepository;
import com.rentalhub.repository.TenantRepository;
import com.rentalhub.repository.UserRepository;
import com.rentalhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final PropertyRepository           propertyRepository;
    private final TenantRepository             tenantRepository;
    private final PaymentRepository            paymentRepository;
    private final AgreementRepository          agreementRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final UserRepository               userRepository;

    // ── ADMIN ─────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard() {
        log.debug("Building admin dashboard");

        LocalDate now   = LocalDate.now();
        int month       = now.getMonthValue();
        int year        = now.getYear();

        long totalUsers    = userRepository.count();
        long totalOwners   = userRepository.countByRoleName(RoleName.ROLE_OWNER);
        long totalTenants  = userRepository.countByRoleName(RoleName.ROLE_TENANT);

        long totalProperties    = propertyRepository.count();
        long occupiedProperties = propertyRepository.countByStatus(PropertyStatus.OCCUPIED);
        long vacantProperties   = propertyRepository.countByStatus(PropertyStatus.AVAILABLE);

        BigDecimal revenueThisMonth = paymentRepository.sumPaidForMonth(month, year);
        BigDecimal revenueThisYear  = calculateYearlyRevenue(year);

        long totalAgreements  = agreementRepository.count();
        long activeAgreements = agreementRepository
            .findAll().stream().filter(a -> a.getIsActive()).count();

        long openMaintenance  = maintenanceRequestRepository.countByStatus(MaintenanceStatus.OPEN);
        long totalMaintenance = maintenanceRequestRepository.count();

        // Monthly revenue chart — last 12 months
        List<AdminDashboardDTO.MonthlyRevenuePoint> monthlyRevenue =
            buildPlatformMonthlyRevenue(year);

        return ResponseEntity.ok(AdminDashboardDTO.builder()
            .totalUsers(totalUsers)
            .totalOwners(totalOwners)
            .totalTenants(totalTenants)
            .totalProperties(totalProperties)
            .occupiedProperties(occupiedProperties)
            .vacantProperties(vacantProperties)
            .totalRevenueThisMonth(
                revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO)
            .totalRevenueThisYear(revenueThisYear)
            .totalAgreements(totalAgreements)
            .activeAgreements(activeAgreements)
            .openMaintenanceRequests(openMaintenance)
            .totalMaintenanceRequests(totalMaintenance)
            .monthlyRevenue(monthlyRevenue)
            .build());
    }

    // ── OWNER ──────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    @GetMapping("/owner")
    @Transactional(readOnly = true)
    public ResponseEntity<OwnerDashboardDTO> getOwnerDashboard() {
        Long ownerId = SecurityUtils.getCurrentUserId();
        log.debug("Building owner dashboard for userId={}", ownerId);

        LocalDate now   = LocalDate.now();
        int month       = now.getMonthValue();
        int year        = now.getYear();

        // Properties
        long totalProps    = propertyRepository.countByOwnerId(ownerId);
        long occupiedProps = propertyRepository.countByOwnerIdAndStatus(
            ownerId, PropertyStatus.OCCUPIED);
        long vacantProps   = propertyRepository.countByOwnerIdAndStatus(
            ownerId, PropertyStatus.AVAILABLE);

        int occupancyRate = totalProps > 0
            ? (int) Math.round((double) occupiedProps / totalProps * 100)
            : 0;

        // Tenants
        long activeTenants = tenantRepository.countCurrentTenantsByOwner(ownerId);

        // Payments
        BigDecimal collected = paymentRepository.sumPaidByOwnerAndMonth(ownerId, month, year);
        BigDecimal pending   = paymentRepository.sumPendingByOwner(ownerId);

        // Overdue amount
        BigDecimal overdueAmount = paymentRepository
            .findByPropertyOwnerIdAndStatus(ownerId, PaymentStatus.OVERDUE)
            .stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount    = paymentRepository
            .countByPropertyOwnerIdAndStatus(ownerId, PaymentStatus.PAID);
        long pendingCount = paymentRepository
            .countByPropertyOwnerIdAndStatus(ownerId, PaymentStatus.PENDING);
        long overdueCount = paymentRepository
            .countByPropertyOwnerIdAndStatus(ownerId, PaymentStatus.OVERDUE);

        // Agreements
        List<Agreement> ownerAgreements = agreementRepository
            .findByOwnerIdOrderByStartDateDesc(ownerId, org.springframework.data.domain.Pageable.unpaged())
            .getContent();

        long activeAgreements = ownerAgreements.stream()
            .filter(Agreement::getIsActive).count();

        long expiringSoon = ownerAgreements.stream()
            .filter(a -> a.getIsActive() && a.isExpiringSoon(30))
            .count();

        // Maintenance
        long openRequests = maintenanceRequestRepository
            .countByPropertyOwnerIdAndStatus(ownerId, MaintenanceStatus.OPEN);
        long inProgressRequests = maintenanceRequestRepository
            .countByPropertyOwnerIdAndStatus(ownerId, MaintenanceStatus.IN_PROGRESS);

        // Monthly revenue chart — last 6 months
        List<AdminDashboardDTO.MonthlyRevenuePoint> monthlyRevenue =
            buildOwnerMonthlyRevenue(ownerId, year);

        // Property occupancy list
        List<OwnerDashboardDTO.PropertyOccupancyPoint> propertyList =
            propertyRepository.findByOwnerId(ownerId,
                org.springframework.data.domain.Pageable.ofSize(50))
            .getContent()
            .stream()
            .map(p -> OwnerDashboardDTO.PropertyOccupancyPoint.builder()
                .propertyName(p.getPropertyName())
                .status(p.getStatus().name())
                .rentAmount(p.getRentAmount())
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(OwnerDashboardDTO.builder()
            .totalProperties(totalProps)
            .occupiedProperties(occupiedProps)
            .vacantProperties(vacantProps)
            .occupancyRate(occupancyRate)
            .activeTenants(activeTenants)
            .collectedThisMonth(collected != null ? collected : BigDecimal.ZERO)
            .pendingAmount(pending != null ? pending : BigDecimal.ZERO)
            .overdueAmount(overdueAmount)
            .paidCount(paidCount)
            .pendingCount(pendingCount)
            .overdueCount(overdueCount)
            .activeAgreements(activeAgreements)
            .expiringSoonCount(expiringSoon)
            .openRequests(openRequests)
            .inProgressRequests(inProgressRequests)
            .monthlyRevenue(monthlyRevenue)
            .propertyOccupancy(propertyList)
            .build());
    }

    // ── TENANT ─────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('ROLE_TENANT')")
    @GetMapping("/tenant")
    @Transactional(readOnly = true)
    public ResponseEntity<TenantDashboardDTO> getTenantDashboard() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.debug("Building tenant dashboard for userId={}", userId);

        // ── FIXED: use findFirst to avoid crash on duplicate records ──
        Tenant tenant = tenantRepository
            .findFirstByTenantUserIdAndMoveOutDateIsNullOrderByMoveInDateDesc(userId)
            .orElseThrow(() -> new BadRequestException(
                "No active tenancy found for your account. " +
                "Please contact your property owner."));

        Property property = tenant.getProperty();
        LocalDate now     = LocalDate.now();

        Payment currentPayment = paymentRepository
            .findByPropertyIdAndTenantIdAndRentMonthAndRentYear(
                property.getId(), tenant.getId(),
                now.getMonthValue(), now.getYear())
            .orElse(null);

        Agreement agreement = agreementRepository
            .findByTenantIdAndIsActiveTrue(tenant.getId())
            .orElse(null);

        String    agreementStatus = "NONE";
        long      daysLeft        = 0;
        LocalDate agreementEnd    = null;

        if (agreement != null) {
            agreementEnd    = agreement.getEndDate();
            daysLeft        = ChronoUnit.DAYS.between(now, agreementEnd);
            agreementStatus = agreement.getIsExpired() ? "EXPIRED"
                            : daysLeft <= 30           ? "EXPIRING_SOON"
                            : "ACTIVE";
        }

        long openMaintenance = maintenanceRequestRepository
            .countByTenantIdAndStatus(tenant.getId(), MaintenanceStatus.OPEN);
        long inProgressMaint = maintenanceRequestRepository
            .countByTenantIdAndStatus(tenant.getId(), MaintenanceStatus.IN_PROGRESS);
        long completedMaint  = maintenanceRequestRepository
            .countByTenantIdAndStatus(tenant.getId(), MaintenanceStatus.COMPLETED);

        long daysAsTenant = ChronoUnit.DAYS.between(tenant.getMoveInDate(), now);

        return ResponseEntity.ok(TenantDashboardDTO.builder()
            .propertyId(property.getId())
            .propertyName(property.getPropertyName())
            .propertyAddress(property.getAddressLine1())
            .propertyCity(property.getCity())
            .monthlyRent(property.getRentAmount())
            .depositAmount(property.getDepositAmount())
            .tenantId(tenant.getId())
            .moveInDate(tenant.getMoveInDate())
            .daysAsTenanant(daysAsTenant)
            .currentPaymentId(currentPayment != null ? currentPayment.getId() : null)
            .currentPaymentStatus(currentPayment != null
                ? currentPayment.getStatus().name() : "NO_RECORD")
            .currentPaymentAmount(currentPayment != null
                ? currentPayment.getAmount() : property.getRentAmount())
            .currentPaymentDueDate(currentPayment != null
                ? currentPayment.getDueDate() : null)
            .currentPaymentPaidDate(currentPayment != null
                ? currentPayment.getPaidDate() : null)
            .agreementId(agreement != null ? agreement.getId() : null)
            .agreementStatus(agreementStatus)
            .agreementEndDate(agreementEnd)
            .agreementDaysLeft(daysLeft)
            .openMaintenanceRequests(openMaintenance)
            .inProgressRequests(inProgressMaint)
            .totalMaintenanceRequests(openMaintenance + inProgressMaint + completedMaint)
            .build());
    }
    // ── Private helpers ───────────────────────────────────────────

    private BigDecimal calculateYearlyRevenue(int year) {
        BigDecimal total = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            BigDecimal monthly = paymentRepository.sumPaidForMonth(m, year);
            if (monthly != null) total = total.add(monthly);
        }
        return total;
    }

    private List<AdminDashboardDTO.MonthlyRevenuePoint> buildPlatformMonthlyRevenue(int year) {
        List<AdminDashboardDTO.MonthlyRevenuePoint> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal amount = paymentRepository.sumPaidForMonth(m, year);
            result.add(AdminDashboardDTO.MonthlyRevenuePoint.builder()
                .month(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .build());
        }
        return result;
    }

    private List<AdminDashboardDTO.MonthlyRevenuePoint> buildOwnerMonthlyRevenue(
            Long ownerId, int year) {
        List<AdminDashboardDTO.MonthlyRevenuePoint> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal amount = paymentRepository.sumPaidByOwnerAndMonth(ownerId, m, year);
            result.add(AdminDashboardDTO.MonthlyRevenuePoint.builder()
                .month(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .build());
        }
        return result;
    }
}