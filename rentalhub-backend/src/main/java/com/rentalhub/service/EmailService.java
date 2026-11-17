package com.rentalhub.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    
    @Async("emailTaskExecutor")
    public void sendTemplatedEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> variables) {

        try {
            // Build Thymeleaf context with the variables
            Context context = new Context();
            context.setVariables(variables);

            // Process template → HTML string
            String html = templateEngine.process("email/" + templateName, context);

            // Build MIME message (supports HTML content)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = isHtml

            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);

        } catch (MessagingException e) {
            /*
             * Email failures are logged but NOT re-thrown.
             * An email failure should NEVER roll back a business
             * transaction. The payment is still recorded even if
             * the confirmation email fails to send.
             */
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── Convenience methods ───────────────────────────────────────
    // Each wraps sendTemplatedEmail with the right template + subject.

    @Async("emailTaskExecutor")
    public void sendPaymentConfirmation(
            String to, String tenantName, String propertyName,
            String monthLabel, String amount) {
        sendTemplatedEmail(to,
            "Rent Payment Confirmed — " + monthLabel,
            "payment-confirmed",
            Map.of(
                "tenantName",   tenantName,
                "propertyName", propertyName,
                "monthLabel",   monthLabel,
                "amount",       amount
            ));
    }

    @Async("emailTaskExecutor")
    public void sendRentDueReminder(
            String to, String tenantName, String propertyName,
            String monthLabel, String amount, long daysUntilDue) {
        sendTemplatedEmail(to,
            "Rent Due Reminder — " + monthLabel,
            "rent-due",
            Map.of(
                "tenantName",   tenantName,
                "propertyName", propertyName,
                "monthLabel",   monthLabel,
                "amount",       amount,
                "daysUntilDue", daysUntilDue
            ));
    }

    @Async("emailTaskExecutor")
    public void sendMaintenanceUpdate(
            String to, String tenantName, String title,
            String status, String resolutionNotes) {
        sendTemplatedEmail(to,
            "Maintenance Request Update — " + title,
            "maintenance-update",
            Map.of(
                "tenantName",       tenantName,
                "requestTitle",     title,
                "status",           status,
                "resolutionNotes",  resolutionNotes != null ? resolutionNotes : ""
            ));
    }

    @Async("emailTaskExecutor")
    public void sendAgreementExpiryAlert(
            String to, String ownerName, String tenantName,
            String propertyName, long daysUntilExpiry, String endDate) {
        sendTemplatedEmail(to,
            "Agreement Expiring Soon — " + propertyName,
            "agreement-expiry",
            Map.of(
                "ownerName",       ownerName,
                "tenantName",      tenantName,
                "propertyName",    propertyName,
                "daysUntilExpiry", daysUntilExpiry,
                "endDate",         endDate
            ));
    }
}