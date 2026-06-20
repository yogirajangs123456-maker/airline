package com.skyway.airline.service;

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
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.skyway.airline.entity.Passenger;
import com.skyway.airline.entity.Reservation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

        private static final DeviceRgb BRAND_BLUE = new DeviceRgb(0, 102, 204);
        private static final DeviceRgb BRAND_LIGHT = new DeviceRgb(235, 245, 255);
        private static final DeviceRgb GREY_TEXT = new DeviceRgb(80, 80, 80);
        private static final DeviceRgb DIVIDER = new DeviceRgb(200, 200, 200);

        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
        private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        public byte[] generateTicket(Reservation reservation) {
                try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdfDoc = new PdfDocument(writer);
                        Document document = new Document(pdfDoc, PageSize.A4);
                        document.setMargins(30, 40, 30, 40);

                        PdfFont bold = PdfFontFactory
                                        .createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
                        PdfFont regular = PdfFontFactory
                                        .createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

                        // ── Header bar ──────────────────────────────────────────
                        Table header = new Table(UnitValue.createPercentArray(new float[] { 70, 30 }))
                                        .setWidth(UnitValue.createPercentValue(100));

                        Cell titleCell = new Cell()
                                        .add(new Paragraph("✈  SkyWay Airlines")
                                                        .setFont(bold).setFontSize(22)
                                                        .setFontColor(ColorConstants.WHITE))
                                        .add(new Paragraph("Electronic Ticket / Boarding Pass")
                                                        .setFont(regular).setFontSize(10)
                                                        .setFontColor(ColorConstants.WHITE))
                                        .setBackgroundColor(BRAND_BLUE).setPadding(16).setBorder(Border.NO_BORDER);

                        Cell pnrCell = new Cell()
                                        .add(new Paragraph("PNR")
                                                        .setFont(regular).setFontSize(10)
                                                        .setFontColor(ColorConstants.WHITE)
                                                        .setTextAlignment(TextAlignment.RIGHT))
                                        .add(new Paragraph(reservation.getPnr())
                                                        .setFont(bold).setFontSize(18)
                                                        .setFontColor(ColorConstants.WHITE)
                                                        .setTextAlignment(TextAlignment.RIGHT))
                                        .setBackgroundColor(BRAND_BLUE).setPadding(16).setBorder(Border.NO_BORDER)
                                        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

                        header.addCell(titleCell);
                        header.addCell(pnrCell);
                        document.add(header);

                        // ── Status badge ────────────────────────────────────────
                        boolean isCancelled = "CANCELLED".equals(reservation.getStatus());
                        DeviceRgb statusBg = isCancelled ? new DeviceRgb(220, 53, 69) : new DeviceRgb(25, 135, 84);
                        Paragraph statusBadge = new Paragraph(
                                        isCancelled ? "  ✗  CANCELLED  " : "  ✓  " + reservation.getStatus() + "  ")
                                        .setFont(bold).setFontSize(11).setFontColor(ColorConstants.WHITE)
                                        .setBackgroundColor(statusBg).setPadding(4).setMarginTop(8)
                                        .setTextAlignment(TextAlignment.CENTER);
                        document.add(statusBadge);

                        // ── Flight route block ──────────────────────────────────
                        document.add(new Paragraph("\n"));
                        Table routeTable = new Table(UnitValue.createPercentArray(new float[] { 40, 20, 40 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setBackgroundColor(BRAND_LIGHT)
                                        .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

                        Cell fromCell = new Cell()
                                        .add(new Paragraph(reservation.getFlight().getSource())
                                                        .setFont(bold).setFontSize(28).setFontColor(BRAND_BLUE))
                                        .add(new Paragraph(reservation.getFlight().getDepartureTime().format(TIME_FMT))
                                                        .setFont(bold).setFontSize(16).setFontColor(GREY_TEXT))
                                        .add(new Paragraph(reservation.getFlight().getJourneyDate().format(DATE_FMT))
                                                        .setFont(regular).setFontSize(10).setFontColor(GREY_TEXT))
                                        .setPadding(14).setBorder(Border.NO_BORDER);

                        Cell arrowCell = new Cell()
                                        .add(new Paragraph("──✈──")
                                                        .setFont(bold).setFontSize(14).setFontColor(BRAND_BLUE)
                                                        .setTextAlignment(TextAlignment.CENTER))
                                        .add(new Paragraph(reservation.getFlight().getDuration() != null
                                                        ? reservation.getFlight().getDuration()
                                                        : "")
                                                        .setFont(regular).setFontSize(9).setFontColor(GREY_TEXT)
                                                        .setTextAlignment(TextAlignment.CENTER))
                                        .setPadding(14).setBorder(Border.NO_BORDER)
                                        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

                        Cell toCell = new Cell()
                                        .add(new Paragraph(reservation.getFlight().getDestination())
                                                        .setFont(bold).setFontSize(28).setFontColor(BRAND_BLUE)
                                                        .setTextAlignment(TextAlignment.RIGHT))
                                        .add(new Paragraph(reservation.getFlight().getArrivalTime().format(TIME_FMT))
                                                        .setFont(bold).setFontSize(16).setFontColor(GREY_TEXT)
                                                        .setTextAlignment(TextAlignment.RIGHT))
                                        .add(new Paragraph(" ").setFont(regular).setFontSize(10))
                                        .setPadding(14).setBorder(Border.NO_BORDER);

                        routeTable.addCell(fromCell);
                        routeTable.addCell(arrowCell);
                        routeTable.addCell(toCell);
                        document.add(routeTable);

                        // ── Flight number / airline row ──────────────────────────
                        document.add(new Paragraph("\n"));
                        Table flightMeta = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }))
                                        .setWidth(UnitValue.createPercentValue(100));
                        flightMeta.addCell(metaCell("Flight No.",
                                        reservation.getFlight().getAirlineCode() + " "
                                                        + reservation.getFlight().getFlightNumber(),
                                        bold, regular));
                        flightMeta.addCell(
                                        metaCell("Airline", reservation.getFlight().getAirlineName(), bold, regular));
                        document.add(flightMeta);

                        // ── Passenger list ────────────────────────────────────────
                        document.add(new Paragraph("\n"));
                        document.add(new Paragraph("Passengers")
                                        .setFont(bold).setFontSize(13).setFontColor(BRAND_BLUE).setMarginBottom(6));

                        Table passengerTable = new Table(UnitValue.createPercentArray(new float[] { 50, 25, 25 }))
                                        .setWidth(UnitValue.createPercentValue(100));

                        passengerTable.addHeaderCell(headerCell("Passenger", bold));
                        passengerTable.addHeaderCell(headerCell("Seat", bold));
                        passengerTable.addHeaderCell(headerCell("Status", bold));

                        for (Passenger p : reservation.getPassengers()) {
                                boolean pCancelled = "CANCELLED".equals(p.getStatus());
                                passengerTable.addCell(rowCell(p.getPassengerName(), regular, pCancelled));
                                passengerTable.addCell(rowCell(p.getSeatNumber(), regular, pCancelled));
                                passengerTable.addCell(rowCell(p.getStatus(), regular, pCancelled));
                        }
                        document.add(passengerTable);

                        // ── Price / refund section ──────────────────────────────
                        document.add(new Paragraph("\n"));
                        document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                                        .setStrokeColor(DIVIDER));

                        Table priceTable = new Table(UnitValue.createPercentArray(new float[] { 70, 30 }))
                                        .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);

                        priceTable.addCell(new Cell()
                                        .add(new Paragraph("Total Fare Paid")
                                                        .setFont(regular).setFontSize(11).setFontColor(GREY_TEXT))
                                        .setBorder(Border.NO_BORDER));
                        priceTable.addCell(new Cell()
                                        .add(new Paragraph("₹ " + reservation.getTotalPrice()
                                                        .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                                        .setFont(bold).setFontSize(16).setFontColor(BRAND_BLUE)
                                                        .setTextAlignment(TextAlignment.RIGHT))
                                        .setBorder(Border.NO_BORDER));
                        document.add(priceTable);

                        if (reservation.getRefundAmount() != null
                                        && reservation.getRefundAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                                Table refundTable = new Table(UnitValue.createPercentArray(new float[] { 70, 30 }))
                                                .setWidth(UnitValue.createPercentValue(100));
                                refundTable.addCell(new Cell()
                                                .add(new Paragraph("Refund Amount")
                                                                .setFont(regular).setFontSize(11)
                                                                .setFontColor(GREY_TEXT))
                                                .setBorder(Border.NO_BORDER));
                                refundTable.addCell(new Cell()
                                                .add(new Paragraph("₹ " + reservation.getRefundAmount()
                                                                .setScale(2, java.math.RoundingMode.HALF_UP)
                                                                .toPlainString() + " Refunded")
                                                                .setFont(bold).setFontSize(13)
                                                                .setFontColor(new DeviceRgb(220, 53, 69))
                                                                .setTextAlignment(TextAlignment.RIGHT))
                                                .setBorder(Border.NO_BORDER));
                                document.add(refundTable);
                        }

                        // ── Booking timestamp ────────────────────────────────────
                        document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                                        .setStrokeColor(DIVIDER).setMarginTop(6));
                        document.add(new Paragraph(
                                        "Booked on: " + reservation.getBookedAt().format(DATETIME_FMT) +
                                                        (reservation.getCancelledAt() != null
                                                                        ? "     |     Cancelled on: " + reservation
                                                                                        .getCancelledAt()
                                                                                        .format(DATETIME_FMT)
                                                                        : ""))
                                        .setFont(regular).setFontSize(9).setFontColor(GREY_TEXT).setMarginTop(6));

                        // ── Footer ───────────────────────────────────────────────
                        document.add(new Paragraph(
                                        "This is a computer-generated ticket and does not require a signature.\n" +
                                                        "Please carry a valid government-issued photo ID for boarding.")
                                        .setFont(regular).setFontSize(8).setFontColor(new DeviceRgb(150, 150, 150))
                                        .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));

                        document.close();
                        return baos.toByteArray();

                } catch (Exception e) {
                        throw new RuntimeException("Failed to generate PDF ticket: " + e.getMessage(), e);
                }
        }

        private Cell metaCell(String label, String value, PdfFont bold, PdfFont regular) {
                return new Cell()
                                .add(new Paragraph(label).setFont(regular).setFontSize(9).setFontColor(GREY_TEXT))
                                .add(new Paragraph(value != null ? value : "—").setFont(bold).setFontSize(12)
                                                .setFontColor(ColorConstants.BLACK))
                                .setPaddingTop(6).setPaddingBottom(6).setBorder(Border.NO_BORDER);
        }

        private Cell headerCell(String text, PdfFont bold) {
                return new Cell()
                                .add(new Paragraph(text).setFont(bold).setFontSize(10)
                                                .setFontColor(ColorConstants.WHITE))
                                .setBackgroundColor(BRAND_BLUE).setPadding(6).setBorder(Border.NO_BORDER);
        }

        private Cell rowCell(String text, PdfFont regular, boolean cancelled) {
                return new Cell()
                                .add(new Paragraph(text != null ? text : "—")
                                                .setFont(regular).setFontSize(10)
                                                .setFontColor(cancelled ? new DeviceRgb(220, 53, 69)
                                                                : ColorConstants.BLACK))
                                .setPadding(6)
                                .setBorderTop(new SolidBorder(DIVIDER, 0.5f))
                                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                                .setBorderBottom(Border.NO_BORDER);
        }
}