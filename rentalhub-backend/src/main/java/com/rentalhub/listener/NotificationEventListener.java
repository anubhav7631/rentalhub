package com.rentalhub.listener;

import com.rentalhub.entity.enums.NotificationType;
import com.rentalhub.event.AgreementExpiryEvent;
import com.rentalhub.event.MaintenanceStatusChangedEvent;
import com.rentalhub.event.PaymentPaidEvent;
import com.rentalhub.event.RentDueEvent;
import com.rentalhub.service.EmailService;
import com.rentalhub.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * NotificationEventListener — listens for domain events and
 * triggers both in-app notifications and emails.
 *
 * @EventListener registers each method as a listener for the
 * event type matching its parameter.
 *
 * WHY @Transactional HERE?
 * Event listeners run in the same thread as the publisher by default.
 * The publisher's transaction is still active, so accessing lazy
 * relationships on the entities works safely.
 *
 * If you ever switch to @Async event listeners, you'll need
 * a fresh transaction here since the publisher's transaction
 * will have committed before the listener runs.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final InAppNotificationService inAppNotificationService;
    private final EmailService             emailService;

    /**
     * ON PAYMENT PAID:
     *   - In-app notification → tenant: "Your rent has been confirmed"
     *   - Email → tenant: payment confirmation with amount + month
     */
    @EventListener
    @Transactional
    public void onPaymentPaid(PaymentPaidEvent event) {
        var payment  = event.getPayment();
        var tenant   = payment.getTenant();
        var property = payment.getProperty();
        var owner    = property.getOwner();

        String amount = formatCurrency(payment.getAmount().doubleValue());
        String month  = payment.getRentMonth() + "/" + payment.getRentYear();

        // In-app — notify tenant
        if (tenant.getTenantUser() != null) {
            inAppNotificationService.create(
                tenant.getTenantUser().getId(),
                "Rent Payment Confirmed",
                "Your rent of " + amount + " for " + month +
                " at " + property.getPropertyName() + " has been confirmed.",
                NotificationType.RENT_PAID,
                payment.getId(),
                "PAYMENT"
            );

            // Email — tenant
            emailService.sendPaymentConfirmation(
                tenant.getEmail(),
                tenant.getFullName(),
                property.getPropertyName(),
                payment.getRentMonth() + "/" + payment.getRentYear(),
                amount
            );
        }

        log.info("Payment confirmed notifications sent for payment {}", payment.getId());
    }

    /**
     * ON RENT DUE:
     *   - In-app notification → tenant: "Rent due in N days"
     *   - Email → tenant: reminder with amount + due date
     */
    @EventListener
    @Transactional
    public void onRentDue(RentDueEvent event) {
        var payment  = event.getPayment();
        var tenant   = payment.getTenant();
        var property = payment.getProperty();

        String amount = formatCurrency(payment.getAmount().doubleValue());

        if (tenant.getTenantUser() != null) {
            inAppNotificationService.create(
                tenant.getTenantUser().getId(),
                "Rent Due in " + event.getDaysUntilDue() + " Day(s)",
                "Your rent of " + amount + " for " +
                property.getPropertyName() + " is due in " +
                event.getDaysUntilDue() + " day(s).",
                NotificationType.RENT_DUE,
                payment.getId(),
                "PAYMENT"
            );

            emailService.sendRentDueReminder(
                tenant.getEmail(),
                tenant.getFullName(),
                property.getPropertyName(),
                payment.getRentMonth() + "/" + payment.getRentYear(),
                amount,
                event.getDaysUntilDue()
            );
        }

        // Also notify owner
        inAppNotificationService.create(
            property.getOwner().getId(),
            "Rent Due Soon",
            tenant.getFullName() + "'s rent of " + amount + " for " +
            property.getPropertyName() + " is due in " +
            event.getDaysUntilDue() + " day(s).",
            NotificationType.RENT_DUE,
            payment.getId(),
            "PAYMENT"
        );
    }

    /**
     * ON MAINTENANCE STATUS CHANGED:
     *   - In-app → tenant: status update
     *   - Email  → tenant: update with resolution notes
     */
    @EventListener
    @Transactional
    public void onMaintenanceStatusChanged(MaintenanceStatusChangedEvent event) {
        var request  = event.getRequest();
        var tenant   = request.getTenant();
        var property = request.getProperty();

        String statusLabel = event.getNewStatus().name().replace("_", " ");

        // In-app → tenant
        if (tenant.getTenantUser() != null) {
            inAppNotificationService.create(
                tenant.getTenantUser().getId(),
                "Maintenance Request " + statusLabel,
                "Your request \"" + request.getTitle() + "\" at " +
                property.getPropertyName() + " is now " + statusLabel + ".",
                NotificationType.MAINTENANCE_UPDATED,
                request.getId(),
                "MAINTENANCE"
            );

            // Email → tenant
            emailService.sendMaintenanceUpdate(
                tenant.getEmail(),
                tenant.getFullName(),
                request.getTitle(),
                event.getNewStatus().name(),
                request.getResolutionNotes()
            );
        }

        // In-app → owner (if assigned to someone, notify them too)
        if (request.getAssignedTo() != null) {
            inAppNotificationService.create(
                request.getAssignedTo().getId(),
                "Maintenance Assignment Update",
                "Request \"" + request.getTitle() + "\" status: " + statusLabel,
                NotificationType.MAINTENANCE_UPDATED,
                request.getId(),
                "MAINTENANCE"
            );
        }

        log.info("Maintenance notifications sent for request {}", request.getId());
    }

    /**
     * ON AGREEMENT EXPIRY:
     *   - In-app + email → owner: agreement expiring soon
     *   - In-app → tenant: heads up about expiry
     */
    @EventListener
    @Transactional
    public void onAgreementExpiry(AgreementExpiryEvent event) {
        var agreement = event.getAgreement();
        var property  = agreement.getProperty();
        var owner     = property.getOwner();
        var tenant    = agreement.getTenant();

        // In-app + email → owner
        inAppNotificationService.create(
            owner.getId(),
            "Agreement Expiring in " + event.getDaysUntilExpiry() + " Day(s)",
            "The rental agreement for " + property.getPropertyName() +
            " (tenant: " + tenant.getFullName() + ") expires in " +
            event.getDaysUntilExpiry() + " days.",
            NotificationType.AGREEMENT_EXPIRING,
            agreement.getId(),
            "AGREEMENT"
        );

        emailService.sendAgreementExpiryAlert(
            owner.getEmail(),
            owner.getFullName(),
            tenant.getFullName(),
            property.getPropertyName(),
            event.getDaysUntilExpiry(),
            agreement.getEndDate().toString()
        );

        // In-app → tenant
        if (tenant.getTenantUser() != null) {
            inAppNotificationService.create(
                tenant.getTenantUser().getId(),
                "Your Rental Agreement Expiring Soon",
                "Your agreement at " + property.getPropertyName() +
                " expires in " + event.getDaysUntilExpiry() + " days.",
                NotificationType.AGREEMENT_EXPIRING,
                agreement.getId(),
                "AGREEMENT"
            );
        }
    }

    private String formatCurrency(double amount) {
        return "₹" + NumberFormat.getNumberInstance(new Locale("en", "IN"))
            .format((long) amount);
    }
}