package com.rentalhub.service;

import com.rentalhub.entity.Payment;
import com.rentalhub.entity.Property;
import com.rentalhub.entity.Tenant;
import com.rentalhub.entity.MaintenanceRequest;
import com.rentalhub.entity.enums.MaintenanceStatus;
import com.rentalhub.entity.enums.PaymentStatus;
import com.rentalhub.entity.enums.PropertyStatus;
import com.rentalhub.repository.MaintenanceRequestRepository;
import com.rentalhub.repository.PaymentRepository;
import com.rentalhub.repository.PropertyRepository;
import com.rentalhub.repository.TenantRepository;
import com.rentalhub.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * ReportService — generates Excel and PDF reports.
 *
 * Each report method writes directly to the provided OutputStream.
 * The caller (ReportController) provides the response OutputStream
 * via StreamingResponseBody — no byte arrays held in memory.
 *
 * PATTERN:
 *   1. Load data from repositories (scoped to current user's role)
 *   2. Build the file structure (workbook/document)
 *   3. Write to OutputStream
 *   4. Close resources
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    // ── Shared colors ─────────────────────────────────────────────

    private static final DeviceRgb PDF_HEADER_BG =
        new DeviceRgb(55, 48, 163);

    private static final DeviceRgb PDF_ROW_ALT =
        new DeviceRgb(238, 242, 255);

    private static final DeviceRgb PDF_SUCCESS =
        new DeviceRgb(22, 163, 74);

    private static final DeviceRgb PDF_WARNING =
        new DeviceRgb(217, 119, 6);

    private static final DeviceRgb PDF_DANGER =
        new DeviceRgb(220, 38, 38);

    // ════════════════════════════════════════════════════════════════
    // REVENUE REPORT
    // ════════════════════════════════════════════════════════════════

    /**
     * generateRevenueExcel — monthly revenue breakdown.
     *
     * Columns: Month, Property, Tenant, Amount, Status,
     * Paid Date, Reference
     */
    @Transactional(readOnly = true)
    public void generateRevenueExcel(
            int year,
            OutputStream out) throws IOException {

        Long ownerId = SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        // Load all payments for this owner (or all if admin)
        List<Payment> payments = isAdmin
            ? paymentRepository.findAll()
            : paymentRepository.findByPropertyOwnerIdAndYear(
                ownerId,
                year
            );

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet =
                workbook.createSheet("Revenue " + year);

            // Column widths
            sheet.setColumnWidth(0, 4000); // Month
            sheet.setColumnWidth(1, 8000); // Property
            sheet.setColumnWidth(2, 6000); // Tenant
            sheet.setColumnWidth(3, 4000); // Amount
            sheet.setColumnWidth(4, 4000); // Status
            sheet.setColumnWidth(5, 4000); // Paid Date
            sheet.setColumnWidth(6, 6000); // Reference

            // Title row
            CellStyle titleStyle =
                createTitleStyle(workbook);

            Row titleRow = sheet.createRow(0);

            org.apache.poi.ss.usermodel.Cell titleCell =
                titleRow.createCell(0);

            titleCell.setCellValue(
                "Revenue Report — " + year
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 6)
            );

            // Header row
            CellStyle headerStyle =
                createHeaderStyle(workbook);

            Row header = sheet.createRow(1);

            String[] headers = {
                "Month",
                "Property",
                "Tenant",
                "Amount (₹)",
                "Status",
                "Paid Date",
                "Reference"
            };

            for (int i = 0; i < headers.length; i++) {

                org.apache.poi.ss.usermodel.Cell cell =
                    header.createCell(i);

                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            CellStyle normalStyle =
                createNormalStyle(workbook);

            CellStyle altStyle =
                createAltStyle(workbook);

            CellStyle paidStyle =
                createColorStyle(
                    workbook,
                    IndexedColors.GREEN
                );

            CellStyle overdueStyle =
                createColorStyle(
                    workbook,
                    IndexedColors.RED
                );

            int rowNum = 2;
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Payment p : payments) {

                if (p.getRentYear() != year) {
                    continue;
                }

                Row row = sheet.createRow(rowNum);

                CellStyle rowStyle =
                    rowNum % 2 == 0
                        ? normalStyle
                        : altStyle;

                setCell(
                    row,
                    0,
                    monthName(p.getRentMonth()),
                    rowStyle
                );

                setCell(
                    row,
                    1,
                    p.getProperty().getPropertyName(),
                    rowStyle
                );

                setCell(
                    row,
                    2,
                    p.getTenant().getFullName(),
                    rowStyle
                );

                setCell(
                    row,
                    3,
                    p.getAmount().toString(),
                    rowStyle
                );

                // Status cell with color
                CellStyle statusStyle =
                    p.isPaid()
                        ? paidStyle
                        : p.isOverdue()
                            ? overdueStyle
                            : rowStyle;

                setCell(
                    row,
                    4,
                    p.getStatus().name(),
                    statusStyle
                );

                setCell(
                    row,
                    5,
                    p.getPaidDate() != null
                        ? p.getPaidDate().toString()
                        : "",
                    rowStyle
                );

                setCell(
                    row,
                    6,
                    p.getPaymentReference() != null
                        ? p.getPaymentReference()
                        : "",
                    rowStyle
                );

                if (p.isPaid()) {
                    grandTotal =
                        grandTotal.add(p.getAmount());
                }

                rowNum++;
            }

            // Total row
            CellStyle totalStyle =
                createTotalStyle(workbook);

            Row totalRow =
                sheet.createRow(rowNum + 1);

            setCell(
                totalRow,
                2,
                "TOTAL COLLECTED",
                totalStyle
            );

            setCell(
                totalRow,
                3,
                grandTotal.toString(),
                totalStyle
            );

            workbook.write(out);

            log.info(
                "Revenue Excel generated: {} rows for year {}",
                rowNum - 2,
                year
            );
        }
    }

    @Transactional(readOnly = true)
    public void generateRevenuePdf(
            int year,
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<Payment> payments = isAdmin
            ? paymentRepository.findAll()
            : paymentRepository.findByPropertyOwnerIdAndYear(
                ownerId,
                year
            );

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        document.add(
            new Paragraph("Revenue Report — " + year)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
        );

        document.add(
            new Paragraph(
                "Generated: " + LocalDate.now()
            )
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
        );

        // Table
        Table table = new Table(
            UnitValue.createPercentArray(
                new float[]{
                    12, 25, 20, 12, 12, 12, 15
                }
            )
        ).useAllAvailableWidth();

        // Table headers
        String[] headers = {
            "Month",
            "Property",
            "Tenant",
            "Amount",
            "Status",
            "Paid Date",
            "Reference"
        };

        for (String h : headers) {
            table.addHeaderCell(pdfHeaderCell(h));
        }

        // Table data
        int rowNum = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Payment p : payments) {

            if (p.getRentYear() != year) {
                continue;
            }

            boolean alt = rowNum % 2 != 0;

            table.addCell(
                pdfDataCell(
                    monthName(p.getRentMonth()),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    p.getProperty().getPropertyName(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    p.getTenant().getFullName(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    "₹" + p.getAmount(),
                    alt
                )
            );

            // Status with color
            DeviceRgb statusColor =
                p.isPaid()
                    ? PDF_SUCCESS
                    : p.isOverdue()
                        ? PDF_DANGER
                        : PDF_WARNING;

            Cell statusCell = new Cell()
                .add(
                    new Paragraph(p.getStatus().name())
                        .setFontSize(8)
                        .setBold()
                        .setFontColor(statusColor)
                )
                .setBackgroundColor(
                    alt
                        ? PDF_ROW_ALT
                        : ColorConstants.WHITE
                )
                .setPadding(4);

            table.addCell(statusCell);

            table.addCell(
                pdfDataCell(
                    p.getPaidDate() != null
                        ? p.getPaidDate().toString()
                        : "—",
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    p.getPaymentReference() != null
                        ? p.getPaymentReference()
                        : "—",
                    alt
                )
            );

            if (p.isPaid()) {
                grandTotal =
                    grandTotal.add(p.getAmount());
            }

            rowNum++;
        }

        document.add(table);

        // Grand total
        document.add(
            new Paragraph(
                "Total Collected: ₹" + grandTotal
            )
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(16)
        );

        document.close();

        log.info(
            "Revenue PDF generated: {} rows for year {}",
            rowNum,
            year
        );
    }

    // ════════════════════════════════════════════════════════════════
    // TENANT REPORT
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public void generateTenantExcel(
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<Tenant> tenants = isAdmin
            ? tenantRepository.findAll()
            : tenantRepository.findByPropertyOwnerId(
                ownerId
            );

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet =
                workbook.createSheet("Tenants");

            sheet.setColumnWidth(0, 6000);
            sheet.setColumnWidth(1, 7000);
            sheet.setColumnWidth(2, 4500);
            sheet.setColumnWidth(3, 7000);
            sheet.setColumnWidth(4, 4000);
            sheet.setColumnWidth(5, 4000);
            sheet.setColumnWidth(6, 4000);

            // Title
            CellStyle titleStyle =
                createTitleStyle(workbook);

            Row titleRow = sheet.createRow(0);

            org.apache.poi.ss.usermodel.Cell titleCell =
                titleRow.createCell(0);

            titleCell.setCellValue(
                "Tenant Report — " + LocalDate.now()
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 6)
            );

            // Headers
            CellStyle headerStyle =
                createHeaderStyle(workbook);

            Row header = sheet.createRow(1);

            String[] headers = {
                "Full Name",
                "Email",
                "Phone",
                "Property",
                "Move-in Date",
                "Move-out Date",
                "Status"
            };

            for (int i = 0; i < headers.length; i++) {

                org.apache.poi.ss.usermodel.Cell cell =
                    header.createCell(i);

                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle normalStyle =
                createNormalStyle(workbook);

            CellStyle altStyle =
                createAltStyle(workbook);

            int rowNum = 2;

            for (Tenant t : tenants) {

                Row row = sheet.createRow(rowNum);

                CellStyle style =
                    rowNum % 2 == 0
                        ? normalStyle
                        : altStyle;

                setCell(
                    row,
                    0,
                    t.getFullName(),
                    style
                );

                setCell(
                    row,
                    1,
                    t.getEmail(),
                    style
                );

                setCell(
                    row,
                    2,
                    t.getPhone(),
                    style
                );

                setCell(
                    row,
                    3,
                    t.getProperty().getPropertyName(),
                    style
                );

                setCell(
                    row,
                    4,
                    t.getMoveInDate().toString(),
                    style
                );

                setCell(
                    row,
                    5,
                    t.getMoveOutDate() != null
                        ? t.getMoveOutDate().toString()
                        : "",
                    style
                );

                setCell(
                    row,
                    6,
                    t.isCurrentTenant()
                        ? "Active"
                        : "Moved Out",
                    style
                );

                rowNum++;
            }

            workbook.write(out);

            log.info(
                "Tenant Excel generated: {} rows",
                rowNum - 2
            );
        }
    }

    @Transactional(readOnly = true)
    public void generateTenantPdf(
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<Tenant> tenants = isAdmin
            ? tenantRepository.findAll()
            : tenantRepository.findByPropertyOwnerId(
                ownerId
            );

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(
            new Paragraph("Tenant Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4)
        );

        document.add(
            new Paragraph(
                "Generated: " + LocalDate.now()
            )
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
        );

        Table table = new Table(
            UnitValue.createPercentArray(
                new float[]{
                    20, 25, 15, 20, 12, 12, 10
                }
            )
        ).useAllAvailableWidth();

        String[] headers = {
            "Name",
            "Email",
            "Phone",
            "Property",
            "Move-in",
            "Move-out",
            "Status"
        };

        for (String h : headers) {
            table.addHeaderCell(pdfHeaderCell(h));
        }

        int rowNum = 0;

        for (Tenant t : tenants) {

            boolean alt = rowNum % 2 != 0;

            table.addCell(
                pdfDataCell(t.getFullName(), alt)
            );

            table.addCell(
                pdfDataCell(t.getEmail(), alt)
            );

            table.addCell(
                pdfDataCell(t.getPhone(), alt)
            );

            table.addCell(
                pdfDataCell(
                    t.getProperty().getPropertyName(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    t.getMoveInDate().toString(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    t.getMoveOutDate() != null
                        ? t.getMoveOutDate().toString()
                        : "—",
                    alt
                )
            );

            DeviceRgb statusColor =
                t.isCurrentTenant()
                    ? PDF_SUCCESS
                    : PDF_DANGER;

            table.addCell(
                new Cell()
                    .add(
                        new Paragraph(
                            t.isCurrentTenant()
                                ? "Active"
                                : "Ended"
                        )
                            .setFontSize(8)
                            .setBold()
                            .setFontColor(statusColor)
                    )
                    .setBackgroundColor(
                        alt
                            ? PDF_ROW_ALT
                            : ColorConstants.WHITE
                    )
                    .setPadding(4)
            );

            rowNum++;
        }

        document.add(table);
        document.close();

        log.info(
            "Tenant PDF generated: {} rows",
            rowNum
        );
    }

    // ════════════════════════════════════════════════════════════════
    // OCCUPANCY REPORT
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public void generateOccupancyExcel(
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<Property> properties = isAdmin
            ? propertyRepository.findAll()
            : propertyRepository.findByOwnerId(
                ownerId,
                Pageable.unpaged()
            ).getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet =
                workbook.createSheet("Occupancy");

            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 4000);
            sheet.setColumnWidth(4, 5000);
            sheet.setColumnWidth(5, 4000);

            CellStyle titleStyle =
                createTitleStyle(workbook);

            Row titleRow = sheet.createRow(0);

            org.apache.poi.ss.usermodel.Cell titleCell =
                titleRow.createCell(0);

            titleCell.setCellValue(
                "Property Occupancy Report — " +
                LocalDate.now()
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 5)
            );

            CellStyle headerStyle =
                createHeaderStyle(workbook);

            Row header = sheet.createRow(1);

            String[] headers = {
                "Property Name",
                "Type",
                "City",
                "Rent (₹)",
                "Status",
                "Owner"
            };

            for (int i = 0; i < headers.length; i++) {

                org.apache.poi.ss.usermodel.Cell cell =
                    header.createCell(i);

                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle normalStyle =
                createNormalStyle(workbook);

            CellStyle altStyle =
                createAltStyle(workbook);

            CellStyle occupiedStyle =
                createColorStyle(
                    workbook,
                    IndexedColors.LIGHT_BLUE
                );

            CellStyle vacantStyle =
                createColorStyle(
                    workbook,
                    IndexedColors.LIGHT_GREEN
                );

            int rowNum = 2;
            long occupied = 0;
            long vacant = 0;

            for (Property p : properties) {

                Row row = sheet.createRow(rowNum);

                CellStyle rowStyle =
                    rowNum % 2 == 0
                        ? normalStyle
                        : altStyle;

                setCell(
                    row,
                    0,
                    p.getPropertyName(),
                    rowStyle
                );

                setCell(
                    row,
                    1,
                    p.getPropertyType().name(),
                    rowStyle
                );

                setCell(
                    row,
                    2,
                    p.getCity(),
                    rowStyle
                );

                setCell(
                    row,
                    3,
                    p.getRentAmount().toString(),
                    rowStyle
                );

                boolean isOccupied =
                    p.getStatus() ==
                    PropertyStatus.OCCUPIED;

                CellStyle statusStyle =
                    isOccupied
                        ? occupiedStyle
                        : vacantStyle;

                setCell(
                    row,
                    4,
                    p.getStatus().name(),
                    statusStyle
                );

                setCell(
                    row,
                    5,
                    p.getOwner().getFullName(),
                    rowStyle
                );

                if (isOccupied) {
                    occupied++;
                } else {
                    vacant++;
                }

                rowNum++;
            }

            // Summary
            CellStyle totalStyle =
                createTotalStyle(workbook);

            Row summaryRow =
                sheet.createRow(rowNum + 1);

            setCell(
                summaryRow,
                0,
                "Total Properties: " + properties.size(),
                totalStyle
            );

            setCell(
                summaryRow,
                1,
                "Occupied: " + occupied,
                totalStyle
            );

            setCell(
                summaryRow,
                2,
                "Vacant: " + vacant,
                totalStyle
            );

            workbook.write(out);

            log.info(
                "Occupancy Excel generated: {} properties",
                properties.size()
            );
        }
    }

    @Transactional(readOnly = true)
    public void generateOccupancyPdf(
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<Property> properties = isAdmin
            ? propertyRepository.findAll()
            : propertyRepository.findByOwnerId(
                ownerId,
                Pageable.unpaged()
            ).getContent();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(
            new Paragraph("Property Occupancy Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4)
        );

        document.add(
            new Paragraph(
                "Generated: " + LocalDate.now()
            )
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
        );

        // Summary bar
        long occupied = properties.stream()
            .filter(
                p -> p.getStatus() ==
                    PropertyStatus.OCCUPIED
            )
            .count();

        long vacant =
            properties.size() - occupied;

        int rate = properties.isEmpty()
            ? 0
            : (int) Math.round(
                (double) occupied /
                properties.size() *
                100
            );

        document.add(
            new Paragraph(
                "Total: " + properties.size() +
                "   |   Occupied: " + occupied +
                "   |   Vacant: " + vacant +
                "   |   Occupancy Rate: " +
                rate +
                "%"
            )
                .setFontSize(11)
                .setBold()
                .setBackgroundColor(
                    new DeviceRgb(238, 242, 255)
                )
                .setPadding(10)
                .setMarginBottom(16)
        );

        Table table = new Table(
            UnitValue.createPercentArray(
                new float[]{
                    30, 15, 15, 15, 15, 15
                }
            )
        ).useAllAvailableWidth();

        String[] headers = {
            "Property Name",
            "Type",
            "City",
            "Rent (₹)",
            "Status",
            "Owner"
        };

        for (String h : headers) {
            table.addHeaderCell(pdfHeaderCell(h));
        }

        int rowNum = 0;

        for (Property p : properties) {

            boolean alt = rowNum % 2 != 0;

            table.addCell(
                pdfDataCell(
                    p.getPropertyName(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    p.getPropertyType().name(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(p.getCity(), alt)
            );

            table.addCell(
                pdfDataCell(
                    "₹" + p.getRentAmount(),
                    alt
                )
            );

            boolean isOccupied =
                p.getStatus() ==
                PropertyStatus.OCCUPIED;

            DeviceRgb statusColor =
                isOccupied
                    ? PDF_SUCCESS
                    : PDF_WARNING;

            table.addCell(
                new Cell()
                    .add(
                        new Paragraph(
                            p.getStatus().name()
                        )
                            .setFontSize(8)
                            .setBold()
                            .setFontColor(statusColor)
                    )
                    .setBackgroundColor(
                        alt
                            ? PDF_ROW_ALT
                            : ColorConstants.WHITE
                    )
                    .setPadding(4)
            );

            table.addCell(
                pdfDataCell(
                    p.getOwner().getFullName(),
                    alt
                )
            );

            rowNum++;
        }

        document.add(table);
        document.close();

        log.info(
            "Occupancy PDF generated: {} properties",
            properties.size()
        );
    }

    // ════════════════════════════════════════════════════════════════
    // MAINTENANCE REPORT
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public void generateMaintenanceExcel(
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<MaintenanceRequest> requests = isAdmin
            ? maintenanceRequestRepository.findAll()
            : maintenanceRequestRepository
                .findByPropertyOwnerIdOrderByCreatedAtDesc(
                    ownerId,
                    Pageable.unpaged()
                )
                .getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet =
                workbook.createSheet("Maintenance");

            sheet.setColumnWidth(0, 7000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 5000);
            sheet.setColumnWidth(3, 4000);
            sheet.setColumnWidth(4, 4000);
            sheet.setColumnWidth(5, 6000);
            sheet.setColumnWidth(6, 5000);

            CellStyle titleStyle =
                createTitleStyle(workbook);

            Row titleRow = sheet.createRow(0);

            org.apache.poi.ss.usermodel.Cell titleCell =
                titleRow.createCell(0);

            titleCell.setCellValue(
                "Maintenance Report — " +
                LocalDate.now()
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 6)
            );

            CellStyle headerStyle =
                createHeaderStyle(workbook);

            Row header = sheet.createRow(1);

            String[] headers = {
                "Title",
                "Property",
                "Tenant",
                "Category",
                "Status",
                "Resolution Notes",
                "Raised On"
            };

            for (int i = 0; i < headers.length; i++) {

                org.apache.poi.ss.usermodel.Cell cell =
                    header.createCell(i);

                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle normalStyle =
                createNormalStyle(workbook);

            CellStyle altStyle =
                createAltStyle(workbook);

            int rowNum = 2;

            for (MaintenanceRequest r : requests) {

                Row row = sheet.createRow(rowNum);

                CellStyle style =
                    rowNum % 2 == 0
                        ? normalStyle
                        : altStyle;

                setCell(
                    row,
                    0,
                    r.getTitle(),
                    style
                );

                setCell(
                    row,
                    1,
                    r.getProperty().getPropertyName(),
                    style
                );

                setCell(
                    row,
                    2,
                    r.getTenant().getFullName(),
                    style
                );

                setCell(
                    row,
                    3,
                    r.getCategory() != null
                        ? r.getCategory()
                        : "",
                    style
                );

                setCell(
                    row,
                    4,
                    r.getStatus().name(),
                    style
                );

                setCell(
                    row,
                    5,
                    r.getResolutionNotes() != null
                        ? r.getResolutionNotes()
                        : "",
                    style
                );

                setCell(
                    row,
                    6,
                    r.getCreatedAt()
                        .toLocalDate()
                        .toString(),
                    style
                );

                rowNum++;
            }

            // Status summary
            long open = requests.stream()
                .filter(
                    r -> r.getStatus() ==
                        MaintenanceStatus.OPEN
                )
                .count();

            long inProgress = requests.stream()
                .filter(
                    r -> r.getStatus() ==
                        MaintenanceStatus.IN_PROGRESS
                )
                .count();

            long completed = requests.stream()
                .filter(
                    r -> r.getStatus() ==
                        MaintenanceStatus.COMPLETED
                )
                .count();

            long rejected = requests.stream()
                .filter(
                    r -> r.getStatus() ==
                        MaintenanceStatus.REJECTED
                )
                .count();

            CellStyle totalStyle =
                createTotalStyle(workbook);

            Row summaryRow =
                sheet.createRow(rowNum + 1);

            setCell(
                summaryRow,
                0,
                "Open: " + open,
                totalStyle
            );

            setCell(
                summaryRow,
                1,
                "In Progress: " + inProgress,
                totalStyle
            );

            setCell(
                summaryRow,
                2,
                "Completed: " + completed,
                totalStyle
            );

            setCell(
                summaryRow,
                3,
                "Rejected: " + rejected,
                totalStyle
            );

            workbook.write(out);

            log.info(
                "Maintenance Excel generated: {} rows",
                rowNum - 2
            );
        }
    }

    @Transactional(readOnly = true)
    public void generateMaintenancePdf(
            OutputStream out) throws IOException {

        Long ownerId =
            SecurityUtils.getCurrentUserId();

        boolean isAdmin =
            SecurityUtils.hasAuthority("ROLE_ADMIN");

        List<MaintenanceRequest> requests = isAdmin
            ? maintenanceRequestRepository.findAll()
            : maintenanceRequestRepository
                .findByPropertyOwnerIdOrderByCreatedAtDesc(
                    ownerId,
                    Pageable.unpaged()
                )
                .getContent();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(
            new Paragraph("Maintenance Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4)
        );

        document.add(
            new Paragraph(
                "Generated: " + LocalDate.now()
            )
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
        );

        Table table = new Table(
            UnitValue.createPercentArray(
                new float[]{
                    25, 20, 15, 12, 12, 20
                }
            )
        ).useAllAvailableWidth();

        String[] headers = {
            "Title",
            "Property",
            "Tenant",
            "Category",
            "Status",
            "Raised On"
        };

        for (String h : headers) {
            table.addHeaderCell(pdfHeaderCell(h));
        }

        int rowNum = 0;

        for (MaintenanceRequest r : requests) {

            boolean alt = rowNum % 2 != 0;

            table.addCell(
                pdfDataCell(r.getTitle(), alt)
            );

            table.addCell(
                pdfDataCell(
                    r.getProperty().getPropertyName(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    r.getTenant().getFullName(),
                    alt
                )
            );

            table.addCell(
                pdfDataCell(
                    r.getCategory() != null
                        ? r.getCategory()
                        : "—",
                    alt
                )
            );

            DeviceRgb statusColor =
                switch (r.getStatus()) {
                    case OPEN ->
                        PDF_WARNING;
                    case IN_PROGRESS ->
                        new DeviceRgb(37, 99, 235);
                    case COMPLETED ->
                        PDF_SUCCESS;
                    case REJECTED ->
                        PDF_DANGER;
                };

            table.addCell(
                new Cell()
                    .add(
                        new Paragraph(
                            r.getStatus().name()
                        )
                            .setFontSize(8)
                            .setBold()
                            .setFontColor(statusColor)
                    )
                    .setBackgroundColor(
                        alt
                            ? PDF_ROW_ALT
                            : ColorConstants.WHITE
                    )
                    .setPadding(4)
            );

            table.addCell(
                pdfDataCell(
                    r.getCreatedAt()
                        .toLocalDate()
                        .toString(),
                    alt
                )
            );

            rowNum++;
        }

        document.add(table);
        document.close();

        log.info(
            "Maintenance PDF generated: {} rows",
            rowNum
        );
    }

    // ════════════════════════════════════════════════════════════════
    // EXCEL STYLE HELPERS
    // ════════════════════════════════════════════════════════════════

    private CellStyle createTitleStyle(Workbook wb) {

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();

        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(
            IndexedColors.WHITE.getIndex()
        );

        style.setFont(font);

        style.setFillForegroundColor(
            IndexedColors.INDIGO.getIndex()
        );

        style.setFillPattern(
            FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
            HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
            VerticalAlignment.CENTER
        );

        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();

        font.setBold(true);

        font.setColor(
            IndexedColors.WHITE.getIndex()
        );

        style.setFont(font);

        style.setFillForegroundColor(
            IndexedColors.DARK_BLUE.getIndex()
        );

        style.setFillPattern(
            FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
            HorizontalAlignment.CENTER
        );

        style.setBorderBottom(
            BorderStyle.THIN
        );

        style.setBottomBorderColor(
            IndexedColors.WHITE.getIndex()
        );

        return style;
    }

    private CellStyle createNormalStyle(Workbook wb) {

        CellStyle style = wb.createCellStyle();

        style.setBorderBottom(
            BorderStyle.THIN
        );

        style.setBottomBorderColor(
            IndexedColors.GREY_25_PERCENT.getIndex()
        );

        return style;
    }

    private CellStyle createAltStyle(Workbook wb) {

        CellStyle style = wb.createCellStyle();

        style.setFillForegroundColor(
            IndexedColors.LAVENDER.getIndex()
        );

        style.setFillPattern(
            FillPatternType.SOLID_FOREGROUND
        );

        style.setBorderBottom(
            BorderStyle.THIN
        );

        style.setBottomBorderColor(
            IndexedColors.GREY_25_PERCENT.getIndex()
        );

        return style;
    }

    private CellStyle createColorStyle(
            Workbook wb,
            IndexedColors color) {

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();

        font.setBold(true);
        style.setFont(font);

        style.setFillForegroundColor(
            color.getIndex()
        );

        style.setFillPattern(
            FillPatternType.SOLID_FOREGROUND
        );

        return style;
    }

    private CellStyle createTotalStyle(Workbook wb) {

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();

        font.setBold(true);
        font.setFontHeightInPoints((short) 11);

        style.setFont(font);

        style.setFillForegroundColor(
            IndexedColors.LIGHT_YELLOW.getIndex()
        );

        style.setFillPattern(
            FillPatternType.SOLID_FOREGROUND
        );

        style.setBorderTop(
            BorderStyle.MEDIUM
        );

        return style;
    }

    private void setCell(
            Row row,
            int col,
            String value,
            CellStyle style) {

        org.apache.poi.ss.usermodel.Cell cell =
            row.createCell(col);

        cell.setCellValue(
            value != null ? value : ""
        );

        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // PDF CELL HELPERS
    // ════════════════════════════════════════════════════════════════

    private Cell pdfHeaderCell(String text) {

        return new Cell()
            .add(
                new Paragraph(text)
                    .setFontSize(9)
                    .setBold()
                    .setFontColor(
                        ColorConstants.WHITE
                    )
            )
            .setBackgroundColor(PDF_HEADER_BG)
            .setPadding(6)
            .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell pdfDataCell(
            String text,
            boolean alt) {

        return new Cell()
            .add(
                new Paragraph(
                    text != null ? text : "—"
                ).setFontSize(8)
            )
            .setBackgroundColor(
                alt
                    ? PDF_ROW_ALT
                    : ColorConstants.WHITE
            )
            .setPadding(4);
    }

    // ════════════════════════════════════════════════════════════════
    // UTILITIES
    // ════════════════════════════════════════════════════════════════

    private String monthName(int month) {

        return Month.of(month).getDisplayName(
            TextStyle.FULL,
            Locale.ENGLISH
        );
    }
}