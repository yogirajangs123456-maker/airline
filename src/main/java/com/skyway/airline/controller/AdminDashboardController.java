package com.skyway.airline.controller;

import com.skyway.airline.repository.FlightRepository;
import com.skyway.airline.repository.PassengerRepository;
import com.skyway.airline.repository.ReservationRepository;
import com.skyway.airline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final ReservationRepository reservationRepository;
    private final PassengerRepository passengerRepository;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalUsers", userRepository.count());
        summary.put("totalFlights", flightRepository.count());
        summary.put("totalReservations", reservationRepository.count());

        summary.put("activeReservations", passengerRepository.countActiveReservations());
        summary.put("cancelledReservations", passengerRepository.countFullyCancelledReservations());

        BigDecimal totalRevenue = passengerRepository.sumTotalRevenue();
        BigDecimal totalRefunds = passengerRepository.sumTotalRefunds();
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalRefunds", totalRefunds);
        summary.put("netRevenue", totalRevenue.subtract(totalRefunds));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        summary.put("todaysBookings",
                reservationRepository.countBookingsBetween(startOfToday, startOfTomorrow));

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        summary.put("currentMonthBookings",
                reservationRepository.countBookingsBetween(startOfMonth, startOfNextMonth));

        return ResponseEntity.ok(summary);
    }
}