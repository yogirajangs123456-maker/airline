package com.skyway.airline.controller;

import com.skyway.airline.repository.FlightRepository;
import com.skyway.airline.service.ReservationService;
import com.skyway.airline.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightRepository flightRepository;
    // Bug #1: these were injected as method parameters (null at runtime) — moved to
    // fields
    private final ReservationService reservationService;
    private final SeatLockService seatLockService;

    @GetMapping("/search")
    public List<?> search(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false) String date) {
        if (date != null) {
            return flightRepository
                    .findBySourceIgnoreCaseAndDestinationIgnoreCaseAndJourneyDateAndActiveTrue(
                            source, destination, LocalDate.parse(date));
        }
        return flightRepository
                .findBySourceIgnoreCaseContainingAndDestinationIgnoreCaseContainingAndActiveTrue(
                        source, destination);
    }

    @GetMapping("/{id}/seats")
    public Map<String, List<String>> getSeats(@PathVariable Long id) {
        return Map.of(
                "booked", reservationService.getBookedSeats(id),
                "locked", seatLockService.getLockedSeats(id));
    }
}