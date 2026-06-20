package com.skyway.airline.controller;

import com.skyway.airline.dto.BookingRequest;
import com.skyway.airline.entity.Reservation;
import com.skyway.airline.repository.UserRepository;
import com.skyway.airline.service.AuthService;
import com.skyway.airline.service.PdfService;
import com.skyway.airline.service.ReservationService;
import com.skyway.airline.service.SeatLockService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final SeatLockService seatLockService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PdfService pdfService;

    private String getEmailFromHeader(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer "))
            throw new RuntimeException("Unauthorized");
        return authService.extractEmail(bearer.substring(7));
    }

    @PostMapping("/lock-seat")
    public ResponseEntity<?> lockSeat(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String email = getEmailFromHeader(req);
        Long flightId = Long.parseLong(body.get("flightId").toString());
        String seatNum = body.get("seatNumber").toString();
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        boolean locked = seatLockService.acquireLock(flightId, seatNum, userId);
        if (locked)
            return ResponseEntity.ok(Map.of("message", "Seat locked for 10 minutes"));
        return ResponseEntity.status(409)
                .body(Map.of("error", "Seat is currently held by another user"));
    }

    @PostMapping
    public ResponseEntity<?> book(@RequestBody BookingRequest req, HttpServletRequest request) {
        try {
            String email = getEmailFromHeader(request);
            Reservation reservation = reservationService.createReservation(req, email);
            return ResponseEntity.ok(Map.of(
                    "pnr", reservation.getPnr(),
                    "message", "Booking confirmed!",
                    "reservationId", reservation.getReservationId(),
                    "passengerCount", reservation.getPassengers().size()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> myBookings(HttpServletRequest req) {
        String email = getEmailFromHeader(req);
        return ResponseEntity.ok(reservationService.getUserReservations(email));
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<?> getByPnr(@PathVariable String pnr, HttpServletRequest req) {
        String email = getEmailFromHeader(req);
        return ResponseEntity.ok(reservationService.getReservationByPnr(pnr, email));
    }

    @GetMapping("/{pnr}/ticket")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable String pnr, HttpServletRequest req) {
        String email = getEmailFromHeader(req);
        Reservation reservation = reservationService.getReservationByPnr(pnr, email);
        byte[] pdf = pdfService.generateTicket(reservation);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-" + pnr + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}