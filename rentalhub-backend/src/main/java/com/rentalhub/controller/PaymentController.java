package com.rentalhub.controller;

import com.rentalhub.dto.request.PaymentRequestDTO;
import com.rentalhub.dto.request.RecordPaymentRequestDTO;
import com.rentalhub.dto.response.PaymentResponseDTO;
import com.rentalhub.dto.response.PaymentSummaryDTO;
import com.rentalhub.service.PaymentService;
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

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER', 'ROLE_TENANT')")
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createManually(
            @Valid @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(paymentService.createManually(request));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER', 'ROLE_TENANT')")
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDTO>> list(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long tenantId,
            @PageableDefault(size = 12) Pageable pageable) {
        log.info("GET /payments called by user with roles: {}",
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities());
        return ResponseEntity.ok(paymentService.list(propertyId, tenantId, pageable));
    }

    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or @paymentSecurity.isTenantOrOwner(#id, authentication)"
    )
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or @paymentSecurity.isOwner(#id, authentication)"
    )
    @PatchMapping("/{id}/paid")
    public ResponseEntity<PaymentResponseDTO> markPaid(
            @PathVariable Long id,
            @Valid @RequestBody RecordPaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.recordPayment(id, request));
    }

    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or @paymentSecurity.isOwner(#id, authentication)"
    )
    @PatchMapping("/{id}/waive")
    public ResponseEntity<PaymentResponseDTO> waive(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(paymentService.waivePayment(id, reason));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/summary")
    public ResponseEntity<PaymentSummaryDTO> getSummary() {
        return ResponseEntity.ok(paymentService.getSummary());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateForMonth(
            @RequestBody Map<String, Integer> body) {
        int month = body.getOrDefault("month", LocalDate.now().getMonthValue());
        int year  = body.getOrDefault("year",  LocalDate.now().getYear());
        int count = paymentService.generatePaymentsForMonth(month, year);
        return ResponseEntity.ok(Map.of(
            "message", "Generated " + count + " payment records",
            "month", month, "year", year
        ));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/mark-overdue")
    public ResponseEntity<Map<String, String>> triggerOverdue() {
        paymentService.markOverduePayments();
        return ResponseEntity.ok(Map.of("message", "Overdue check completed"));
    }
}