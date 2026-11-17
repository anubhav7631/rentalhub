package com.rentalhub.controller;

import com.rentalhub.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;

/**
 * ReportController — /api/reports
 *
 * All endpoints return StreamingResponseBody — bytes stream
 * directly to the HTTP response without loading the full file
 * into memory first.
 *
 * Two formats per report type:
 *   /excel → .xlsx file download
 *   /pdf   → .pdf file download
 */
@RestController
@RequestMapping("/reports")
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ── Revenue ───────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/revenue/excel")
    public ResponseEntity<StreamingResponseBody> revenueExcel(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().year}") int year) {

        String filename = "revenue-report-" + year + ".xlsx";
        log.info("Generating revenue Excel for year {}", year);

        StreamingResponseBody body = out -> reportService.generateRevenueExcel(year, out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/revenue/pdf")
    public ResponseEntity<StreamingResponseBody> revenuePdf(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().year}") int year) {

        String filename = "revenue-report-" + year + ".pdf";
        log.info("Generating revenue PDF for year {}", year);

        StreamingResponseBody body = out -> reportService.generateRevenuePdf(year, out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(body);
    }

    // ── Tenants ───────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/tenants/excel")
    public ResponseEntity<StreamingResponseBody> tenantExcel() {
        String filename = "tenant-report-" + LocalDate.now() + ".xlsx";
        StreamingResponseBody body = out -> reportService.generateTenantExcel(out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/tenants/pdf")
    public ResponseEntity<StreamingResponseBody> tenantPdf() {
        String filename = "tenant-report-" + LocalDate.now() + ".pdf";
        StreamingResponseBody body = out -> reportService.generateTenantPdf(out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(body);
    }

    // ── Occupancy ─────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/occupancy/excel")
    public ResponseEntity<StreamingResponseBody> occupancyExcel() {
        String filename = "occupancy-report-" + LocalDate.now() + ".xlsx";
        StreamingResponseBody body = out -> reportService.generateOccupancyExcel(out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/occupancy/pdf")
    public ResponseEntity<StreamingResponseBody> occupancyPdf() {
        String filename = "occupancy-report-" + LocalDate.now() + ".pdf";
        StreamingResponseBody body = out -> reportService.generateOccupancyPdf(out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(body);
    }

    // ── Maintenance ───────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/maintenance/excel")
    public ResponseEntity<StreamingResponseBody> maintenanceExcel() {
        String filename = "maintenance-report-" + LocalDate.now() + ".xlsx";
        StreamingResponseBody body = out -> reportService.generateMaintenanceExcel(out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OWNER')")
    @GetMapping("/maintenance/pdf")
    public ResponseEntity<StreamingResponseBody> maintenancePdf() {
        String filename = "maintenance-report-" + LocalDate.now() + ".pdf";
        StreamingResponseBody body = out -> reportService.generateMaintenancePdf(out);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(body);
    }
}