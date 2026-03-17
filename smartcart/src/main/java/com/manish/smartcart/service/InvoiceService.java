package com.manish.smartcart.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.manish.smartcart.dto.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CONCEPT: iText 7 PDF Generation
 *
 * iText uses a pipeline: PdfWriter (output) → PdfDocument (structure) →
 * Document (layout API).
 * Think of it like HTML: Document = <body>, Paragraph =
 * <p>
 * , Table =
 * <table>
 * , Cell =
 * <td>.
 *
 * Real-world Indian e-commerce invoices (Amazon, Flipkart, Myntra) typically
 * include:
 * 1. Company header with GSTIN & registered address
 * 2. Invoice number in a standard format (INV-YYYY-NNNN)
 * 3. Itemized table with S.No, HSN code placeholder, and ₹ symbol
 * 4. Tax breakdown section (CGST + SGST or IGST)
 * 5. Grand total highlighted prominently
 * 6. Terms & conditions / return policy footer
 * 7. "Computer Generated Invoice" disclaimer (no signature required)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

        // ── BRAND COLORS ──────────────────────────────────────────────────────────
        // Matching the email templates' deep purple theme for visual consistency
        private static final DeviceRgb BRAND_PRIMARY = new DeviceRgb(108, 99, 255); // Deep purple
        private static final DeviceRgb BRAND_DARK = new DeviceRgb(30, 30, 60); // Dark navy
        private static final DeviceRgb HEADER_BG = new DeviceRgb(108, 99, 255); // Purple header band
        private static final DeviceRgb TABLE_HEADER_BG = new DeviceRgb(245, 243, 255); // Light purple tint
        private static final DeviceRgb ALTERNATE_ROW = new DeviceRgb(250, 250, 255); // Zebra stripe
        private static final DeviceRgb BORDER_COLOR = new DeviceRgb(220, 220, 235); // Subtle borders
        private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(120, 120, 140); // Secondary text

        private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        private static final DateTimeFormatter INVOICE_NUM = DateTimeFormatter.ofPattern("yyyyMMdd");

        /**
         * Generates a professional, Amazon India-style PDF invoice.
         *
         * @param order The completed order data (from OrderMapper.toOrderResponse)
         * @return Raw PDF bytes — ready to attach to an email via EmailService
         */
        public byte[] generateInvoice(OrderResponse order) {
                log.info("Generating premium PDF invoice for Order #{}", order.getOrderId());

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdfDoc = new PdfDocument(writer);
                        Document document = new Document(pdfDoc, PageSize.A4);
                        document.setMargins(30, 40, 30, 40); // top, right, bottom, left

                        // Load fonts — Helvetica is clean and universally supported
                        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                        PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

                        // ══════════════════════════════════════════════════════════════════
                        // 1. HEADER BAND — Purple banner with company name + "TAX INVOICE"
                        // ══════════════════════════════════════════════════════════════════
                        Table headerBand = new Table(UnitValue.createPercentArray(new float[] { 3, 2 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setBackgroundColor(HEADER_BG)
                                        .setPadding(0);

                        // Left: Company name
                        Cell brandCell = new Cell()
                                        .add(new Paragraph("CognitoCart").setFont(bold).setFontSize(24)
                                                        .setFontColor(ColorConstants.WHITE))
                                        .add(new Paragraph("AI-Driven E-Commerce").setFont(italic).setFontSize(9)
                                                        .setFontColor(new DeviceRgb(200, 200, 255)))
                                        .setBorder(Border.NO_BORDER)
                                        .setPadding(15)
                                        .setVerticalAlignment(VerticalAlignment.MIDDLE);

                        // Right: Invoice label
                        Cell invoiceLabel = new Cell()
                                        .add(new Paragraph("TAX INVOICE").setFont(bold).setFontSize(18)
                                                        .setFontColor(ColorConstants.WHITE)
                                                        .setTextAlignment(TextAlignment.RIGHT))
                                        .setBorder(Border.NO_BORDER)
                                        .setPadding(15)
                                        .setVerticalAlignment(VerticalAlignment.MIDDLE);

                        headerBand.addCell(brandCell);
                        headerBand.addCell(invoiceLabel);
                        document.add(headerBand);

                        // ══════════════════════════════════════════════════════════════════
                        // 2. COMPANY DETAILS + INVOICE META (side by side)
                        // ══════════════════════════════════════════════════════════════════
                        document.add(new Paragraph("\n").setFontSize(4));

                        Table companyMeta = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                        .setWidth(UnitValue.createPercentValue(100));

                        // Left column: Seller/Company details (mandatory for Indian GST invoices)
                        String companyInfo = "CognitoCart Pvt. Ltd.\n"
                                        + "Registered Address: Lucknow, Uttar Pradesh, India\n"
                                        + "GSTIN: 27AABCC1234D1ZE\n"
                                        + "Email: billing@cognitocart.com";

                        Cell companyCell = new Cell()
                                        .add(new Paragraph("From:").setFont(bold).setFontSize(9)
                                                        .setFontColor(BRAND_PRIMARY))
                                        .add(new Paragraph(companyInfo).setFont(regular).setFontSize(8)
                                                        .setFontColor(LIGHT_GRAY))
                                        .setBorder(Border.NO_BORDER)
                                        .setPaddingTop(8);

                        // Right column: Invoice metadata
                        // Generate a professional invoice number: INV-20260317-0013
                        String invoiceNumber = "INV-" + LocalDateTime.now().format(INVOICE_NUM) + "-"
                                        + String.format("%04d", order.getOrderId());

                        String orderDate = order.getOrderDate() != null
                                        ? order.getOrderDate().format(INVOICE_DATE)
                                        : "N/A";

                        String paymentBadge = order.getPaymentStatus() != null
                                        ? order.getPaymentStatus().name()
                                        : "PAID";

                        Cell metaCell = new Cell()
                                        .add(labelValue("Invoice No:", invoiceNumber, bold, regular))
                                        .add(labelValue("Order ID:", "#" + order.getOrderId(), bold, regular))
                                        .add(labelValue("Date:", orderDate, bold, regular))
                                        .add(labelValue("Payment:", paymentBadge, bold, regular))
                                        .setBorder(Border.NO_BORDER)
                                        .setPaddingTop(8)
                                        .setTextAlignment(TextAlignment.RIGHT);

                        companyMeta.addCell(companyCell);
                        companyMeta.addCell(metaCell);
                        document.add(companyMeta);

                        // ── Thin separator line ──────────────────────────────────────────
                        addSeparator(document);

                        // ══════════════════════════════════════════════════════════════════
                        // 3. BILL TO / SHIP TO
                        // ══════════════════════════════════════════════════════════════════
                        Table billShip = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                        .setWidth(UnitValue.createPercentValue(100));

                        // Bill To
                        Cell billTo = new Cell()
                                        .add(new Paragraph("Bill To:").setFont(bold).setFontSize(9)
                                                        .setFontColor(BRAND_PRIMARY))
                                        .add(new Paragraph(safeString(order.getCustomerName())).setFont(bold)
                                                        .setFontSize(10))
                                        .add(new Paragraph(safeString(order.getEmail())).setFont(regular).setFontSize(8)
                                                        .setFontColor(LIGHT_GRAY))
                                        .setBorder(Border.NO_BORDER);

                        // Ship To
                        Cell shipTo = new Cell()
                                        .add(new Paragraph("Ship To:").setFont(bold).setFontSize(9)
                                                        .setFontColor(BRAND_PRIMARY))
                                        .add(new Paragraph(safeString(order.getShippingAddress())).setFont(regular)
                                                        .setFontSize(9))
                                        .setBorder(Border.NO_BORDER);

                        billShip.addCell(billTo);
                        billShip.addCell(shipTo);
                        document.add(billShip);
                        document.add(new Paragraph("\n").setFontSize(6));

                        // ══════════════════════════════════════════════════════════════════
                        // 4. ITEMS TABLE — with S.No, alternating rows, ₹ symbol
                        // ══════════════════════════════════════════════════════════════════
                        // Columns: S.No | Product | Qty | Unit Price (₹) | Subtotal (₹)
                        Table itemTable = new Table(
                                        UnitValue.createPercentArray(new float[] { 0.6f, 4f, 0.8f, 1.5f, 1.5f }))
                                        .setWidth(UnitValue.createPercentValue(100));

                        // Table header
                        String[] headers = { "#", "Product", "Qty", "Unit Price (Rs.)", "Subtotal (Rs.)" };
                        for (String h : headers) {
                                itemTable.addHeaderCell(
                                                new Cell().add(new Paragraph(h).setFont(bold).setFontSize(9)
                                                                .setFontColor(BRAND_DARK))
                                                                .setBackgroundColor(TABLE_HEADER_BG)
                                                                .setBorderBottom(new SolidBorder(BORDER_COLOR, 1))
                                                                .setBorderTop(new SolidBorder(BORDER_COLOR, 1))
                                                                .setBorderLeft(Border.NO_BORDER)
                                                                .setBorderRight(Border.NO_BORDER)
                                                                .setPadding(7));
                        }

                        // Table data rows — zebra striping for readability
                        int rowIndex = 0;
                        BigDecimal subtotalSum = BigDecimal.ZERO;
                        for (var item : order.getItems()) {
                                DeviceRgb rowBg = (rowIndex % 2 == 1) ? ALTERNATE_ROW : null;
                                BigDecimal itemSubtotal = item.getSubtotal() != null ? item.getSubtotal()
                                                : item.getPriceAtPurchase()
                                                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                                subtotalSum = subtotalSum.add(itemSubtotal);

                                itemTable.addCell(itemCell(String.valueOf(rowIndex + 1), regular, rowBg,
                                                TextAlignment.CENTER));
                                itemTable.addCell(itemCell(safeString(item.getProductName()), regular, rowBg,
                                                TextAlignment.LEFT));
                                itemTable.addCell(itemCell(String.valueOf(item.getQuantity()), regular, rowBg,
                                                TextAlignment.CENTER));
                                itemTable.addCell(itemCell(formatCurrency(item.getPriceAtPurchase()), regular, rowBg,
                                                TextAlignment.RIGHT));
                                itemTable.addCell(itemCell(formatCurrency(itemSubtotal), regular, rowBg,
                                                TextAlignment.RIGHT));
                                rowIndex++;
                        }

                        document.add(itemTable);
                        document.add(new Paragraph("\n").setFontSize(4));

                        // ══════════════════════════════════════════════════════════════════
                        // 5. PRICING BREAKDOWN — right-aligned summary panel
                        // Real-world pattern: Subtotal → Discount → Delivery → Tax → TOTAL
                        // ══════════════════════════════════════════════════════════════════
                        Table summaryTable = new Table(UnitValue.createPercentArray(new float[] { 3, 2 }))
                                        .setWidth(UnitValue.createPercentValue(50))
                                        .setHorizontalAlignment(
                                                        com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);

                        // Subtotal (before discounts)
                        addSummaryRow(summaryTable, "Subtotal:", formatCurrency(subtotalSum), regular, regular);

                        // Coupon discount (if applied)
                        if (order.getCouponCode() != null && order.getDiscountAmount() != null
                                        && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                                addSummaryRow(summaryTable, "Discount (" + order.getCouponCode() + "):",
                                                "- " + formatCurrency(order.getDiscountAmount()), regular, regular);
                        }

                        // Delivery fee (handle null → show "FREE")
                        String deliveryDisplay = (order.getDeliveryFee() == null
                                        || order.getDeliveryFee().compareTo(BigDecimal.ZERO) == 0)
                                                        ? "FREE"
                                                        : formatCurrency(order.getDeliveryFee());
                        addSummaryRow(summaryTable, "Delivery:", deliveryDisplay, regular, regular);

                        // Tax placeholder (for future GST integration)
                        addSummaryRow(summaryTable, "Tax (incl.):", "Included", regular, italic);

                        // Final separator before grand total
                        summaryTable.addCell(new Cell(1, 2)
                                        .add(new Paragraph("").setFontSize(1))
                                        .setBorderBottom(new SolidBorder(BRAND_PRIMARY, 1.5f))
                                        .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER)
                                        .setBorderRight(Border.NO_BORDER)
                                        .setPadding(0));

                        // GRAND TOTAL — bold and colored
                        Cell totalLabel = new Cell()
                                        .add(new Paragraph("GRAND TOTAL:").setFont(bold).setFontSize(12)
                                                        .setFontColor(BRAND_DARK))
                                        .setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.LEFT);

                        Cell totalValue = new Cell()
                                        .add(new Paragraph(formatCurrency(order.getTotalAmount())).setFont(bold)
                                                        .setFontSize(14).setFontColor(BRAND_PRIMARY))
                                        .setBorder(Border.NO_BORDER).setPadding(6)
                                        .setTextAlignment(TextAlignment.RIGHT);

                        summaryTable.addCell(totalLabel);
                        summaryTable.addCell(totalValue);

                        document.add(summaryTable);

                        // ══════════════════════════════════════════════════════════════════
                        // 6. FOOTER — Terms, return policy, and disclaimer
                        // ══════════════════════════════════════════════════════════════════
                        addSeparator(document);

                        document.add(new Paragraph("Terms & Conditions")
                                        .setFont(bold).setFontSize(8).setFontColor(BRAND_DARK).setMarginTop(5));

                        document.add(new Paragraph(
                                        "1. Goods once sold are subject to our return policy (7 days from delivery).\n"
                                                        +
                                                        "2. This is a computer-generated invoice and does not require a physical signature.\n"
                                                        +
                                                        "3. For support or disputes, contact us at support@cognitocart.com.")
                                        .setFont(regular).setFontSize(7).setFontColor(LIGHT_GRAY).setMarginBottom(10));

                        // Thank you message — centered
                        document.add(new Paragraph("Thank you for shopping with CognitoCart!")
                                        .setFont(bold).setFontSize(10).setFontColor(BRAND_PRIMARY)
                                        .setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

                        document.add(new Paragraph(
                                        "www.cognitocart.com  |  support@cognitocart.com  |  +91-7880-589908")
                                        .setFont(regular).setFontSize(7).setFontColor(LIGHT_GRAY)
                                        .setTextAlignment(TextAlignment.CENTER));

                        document.close();
                        log.info("Premium PDF invoice generated for Order #{}", order.getOrderId());
                        return baos.toByteArray();

                } catch (Exception e) {
                        log.error("Failed to generate invoice for Order #{}: {}", order.getOrderId(), e.getMessage());
                        throw new RuntimeException("Invoice generation failed.");
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // HELPER METHODS
        // ═══════════════════════════════════════════════════════════════════════════

        /**
         * Creates a compact "Label: Value" paragraph for the invoice metadata section.
         */
        private Paragraph labelValue(String label, String value, PdfFont labelFont, PdfFont valueFont) {
                return new Paragraph()
                                .add(new com.itextpdf.layout.element.Text(label + " ").setFont(labelFont).setFontSize(8)
                                                .setFontColor(LIGHT_GRAY))
                                .add(new com.itextpdf.layout.element.Text(value).setFont(valueFont).setFontSize(9)
                                                .setFontColor(BRAND_DARK))
                                .setMarginBottom(2);
        }

        /**
         * Creates an item table cell with optional background color for zebra striping.
         */
        private Cell itemCell(String text, PdfFont font, DeviceRgb bgColor, TextAlignment alignment) {
                Cell cell = new Cell()
                                .add(new Paragraph(text).setFont(font).setFontSize(9))
                                .setBorderBottom(new SolidBorder(BORDER_COLOR, 0.5f))
                                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER)
                                .setBorderRight(Border.NO_BORDER)
                                .setPadding(6)
                                .setTextAlignment(alignment);
                if (bgColor != null)
                        cell.setBackgroundColor(bgColor);
                return cell;
        }

        /** Adds a label + value row to the pricing summary table. */
        private void addSummaryRow(Table table, String label, String value, PdfFont labelFont, PdfFont valueFont) {
                table.addCell(new Cell()
                                .add(new Paragraph(label).setFont(labelFont).setFontSize(9).setFontColor(LIGHT_GRAY))
                                .setBorder(Border.NO_BORDER).setPadding(3).setTextAlignment(TextAlignment.LEFT));
                table.addCell(new Cell()
                                .add(new Paragraph(value).setFont(valueFont).setFontSize(9).setFontColor(BRAND_DARK))
                                .setBorder(Border.NO_BORDER).setPadding(3).setTextAlignment(TextAlignment.RIGHT));
        }

        /** Adds a thin horizontal separator line — cleaner than empty paragraphs. */
        private void addSeparator(Document document) {
                SolidLine line = new SolidLine(0.5f);
                line.setColor(BORDER_COLOR);
                document.add(new LineSeparator(line).setMarginTop(8).setMarginBottom(8));
        }

        /** Formats a BigDecimal as Indian Rupee string. Handles null gracefully. */
        private String formatCurrency(BigDecimal amount) {
                if (amount == null)
                        return "Rs. 0.00";
                return "Rs. " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }

        /**
         * Safely returns a string or "N/A" if null — prevents NullPointerException on
         * the PDF.
         */
        private String safeString(String value) {
                return value != null ? value : "N/A";
        }
}
