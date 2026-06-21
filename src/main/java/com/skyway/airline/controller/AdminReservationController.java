package com.skyway.airline.controller;

import com.skyway.airline.entity.Reservation;
import com.skyway.airline.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationRepository reservationRepository;

    @GetMapping
    public List<Reservation> getAll() {
        List<Reservation> reservations = reservationRepository.findAllByOrderByBookedAtDesc();
        reservations.forEach(this::forceLoadLazyFields);
        return reservations;
    }

    @GetMapping("/search")
    public List<Reservation> search(
            @RequestParam(required = false) String pnr,
            @RequestParam(required = false) String passengerName,
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String bookingDate) {

        LocalDate parsedDate = (bookingDate != null && !bookingDate.isBlank())
                ? LocalDate.parse(bookingDate)
                : null;

        List<Reservation> results = reservationRepository.adminSearch(
                blankToNull(pnr), blankToNull(passengerName),
                blankToNull(flightNumber), blankToNull(email), parsedDate);

        results.forEach(this::forceLoadLazyFields);
        return results;
    }

    private String blankToNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    private void forceLoadLazyFields(Reservation r) {
        r.getUser().getEmail();
        r.getFlight().getSource();
        r.getPassengers().forEach(p -> p.getStatus());
    }
}