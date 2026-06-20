package com.skyway.airline.controller;

import com.skyway.airline.dto.CancelRequest;
import com.skyway.airline.dto.RefundPreviewRequest;
import com.skyway.airline.entity.Reservation;
import com.skyway.airline.service.AuthService;
import com.skyway.airline.service.OtpService;
import com.skyway.airline.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final ReservationService reservationService;
    private final AuthService authService;

    private String getEmailFromHeader(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer "))
            throw new RuntimeException("Unauthorized");
        return authService.extractEmail(bearer.substring(7));
    }

    /** Step before OTP: show the refund amount for selected passengers */
    @PostMapping("/refund-preview")
    public ResponseEntity<?> refundPreview(@RequestBody RefundPreviewRequest body, HttpServletRequest req) {
        try {
            String email = getEmailFromHeader(req);
            BigDecimal refund = reservationService.calculateRefundPreview(
                    body.getPnr(), body.getPassengerIds(), email);
            return ResponseEntity.ok(Map.of("refundAmount", refund));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            String email = getEmailFromHeader(req);
            String pnr = body.get("pnr");
            otpService.generateAndSendOTP(email, pnr);
            return ResponseEntity.ok(Map.of("message", "OTP sent"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getClass().getName(), "message", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelWithOtp(@RequestBody CancelRequest body, HttpServletRequest req) {
        try {
            String email = getEmailFromHeader(req);
            boolean valid = otpService.verifyOTP(email, body.getOtp());
            if (!valid)
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));

            Reservation cancelled = reservationService.cancelPassengers(
                    body.getPnr(), body.getPassengerIds(), email);

            return ResponseEntity.ok(Map.of(
                    "message", "Selected passengers cancelled successfully",
                    "pnr", cancelled.getPnr(),
                    "status", cancelled.getStatus(),
                    "refundAmount", cancelled.getRefundAmount()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}