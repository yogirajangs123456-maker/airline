package com.skyway.airline.controller;

import com.skyway.airline.dto.CancelRequest;
import com.skyway.airline.entity.Reservation;
import com.skyway.airline.service.AuthService;
import com.skyway.airline.service.OtpService;
import com.skyway.airline.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final ReservationService reservationService;
    private final AuthService authService;

    // Bug #6: was duplicating raw bearer.substring(7) without null-check everywhere
    // Extracted to a shared helper (same pattern as ReservationController)
    private String getEmailFromHeader(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer "))
            throw new RuntimeException("Unauthorized");
        return authService.extractEmail(bearer.substring(7));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        try {

            System.out.println("STEP 1");

            String email = getEmailFromHeader(req);
            System.out.println("EMAIL = " + email);

            String pnr = body.get("pnr");
            System.out.println("PNR = " + pnr);

            otpService.generateAndSendOTP(email, pnr);

            System.out.println("OTP SENT");

            return ResponseEntity.ok(
                    Map.of("message", "OTP sent"));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", e.getClass().getName(),
                            "message", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelWithOtp(@RequestBody CancelRequest body,
            HttpServletRequest req) {
        try {
            String email = getEmailFromHeader(req);
            boolean valid = otpService.verifyOTP(email, body.getOtp());
            if (!valid)
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));

            Reservation cancelled = reservationService.cancelReservation(body.getPnr(), email);
            return ResponseEntity.ok(Map.of(
                    "message", "Ticket cancelled successfully",
                    "pnr", cancelled.getPnr(),
                    "status", cancelled.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}