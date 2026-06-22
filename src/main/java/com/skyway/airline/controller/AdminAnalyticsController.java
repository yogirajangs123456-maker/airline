package com.skyway.airline.controller;

import com.skyway.airline.dto.FlightDemandDto;
import com.skyway.airline.dto.RouteAnalyticsDto;
import com.skyway.airline.dto.TopCustomerDto;
import com.skyway.airline.entity.Flight;
import com.skyway.airline.entity.Passenger;
import com.skyway.airline.entity.Reservation;
import com.skyway.airline.repository.FlightRepository;
import com.skyway.airline.repository.PassengerRepository;
import com.skyway.airline.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final ReservationRepository reservationRepository;
    private final FlightRepository flightRepository;
    private final PassengerRepository passengerRepository;

    // ── 1. Revenue Analytics (dynamic period) ──
    @GetMapping("/revenue")
    @Transactional(readOnly = true)
    public Map<String, Object> revenueAnalytics(@RequestParam int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        BigDecimal totalRevenue = reservationRepository.sumRevenueSince(start);
        BigDecimal totalRefunds = reservationRepository.sumRefundsSince(start);

        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("totalRevenue", totalRevenue);
        result.put("totalRefunds", totalRefunds);
        result.put("netRevenue", totalRevenue.subtract(totalRefunds));
        return result;
    }

    // ── 2. Route Analytics ──
    @GetMapping("/routes")
    @Transactional(readOnly = true)
    public Map<String, List<RouteAnalyticsDto>> routeAnalytics() {
        List<Reservation> all = reservationRepository.findAll();
        all.forEach(this::forceLoad);

        Map<String, List<Reservation>> byRoute = all.stream()
                .collect(
                        Collectors.groupingBy(r -> r.getFlight().getSource() + " → " + r.getFlight().getDestination()));

        List<RouteAnalyticsDto> routeStats = byRoute.entrySet().stream()
                .map(entry -> {
                    String route = entry.getKey();
                    List<Reservation> reservations = entry.getValue();
                    long bookings = reservations.size();
                    BigDecimal revenue = reservations.stream()
                            .map(Reservation::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long cancellations = reservations.stream()
                            .flatMap(r -> r.getPassengers().stream())
                            .filter(p -> "CANCELLED".equals(p.getStatus()))
                            .count();
                    return new RouteAnalyticsDto(route, bookings, revenue, cancellations);
                })
                .toList();

        Map<String, List<RouteAnalyticsDto>> result = new HashMap<>();
        result.put("mostPopular",
                sortedTopN(routeStats, Comparator.comparingLong(RouteAnalyticsDto::getBookings).reversed(), 5));
        result.put("highestRevenue",
                sortedTopN(routeStats, Comparator.comparing(RouteAnalyticsDto::getRevenue).reversed(), 5));
        result.put("lowestRevenue", sortedTopN(routeStats, Comparator.comparing(RouteAnalyticsDto::getRevenue), 5));
        result.put("mostCancelled",
                sortedTopN(routeStats, Comparator.comparingLong(RouteAnalyticsDto::getCancellations).reversed(), 5));
        return result;
    }

    // ── 3. Flight Demand Analytics ──
    @GetMapping("/flight-demand")
    @Transactional(readOnly = true)
    public Map<String, List<FlightDemandDto>> flightDemandAnalytics() {
        List<Flight> flights = flightRepository.findAll();
        List<Reservation> all = reservationRepository.findAll();
        all.forEach(this::forceLoad);

        Map<String, Long> bookingsByFlightNumber = all.stream()
                .collect(Collectors.groupingBy(r -> r.getFlight().getFlightNumber(), Collectors.counting()));

        List<FlightDemandDto> demandStats = flights.stream()
                .collect(Collectors.groupingBy(Flight::getFlightNumber))
                .entrySet().stream()
                .map(entry -> {
                    String flightNumber = entry.getKey();
                    List<Flight> instances = entry.getValue();
                    long bookings = bookingsByFlightNumber.getOrDefault(flightNumber, 0L);

                    int totalCapacity = instances.stream().mapToInt(Flight::getTotalSeats).sum();
                    int totalAvailable = instances.stream().mapToInt(Flight::getAvailableSeats).sum();
                    double occupancy = totalCapacity > 0
                            ? ((double) (totalCapacity - totalAvailable) / totalCapacity) * 100
                            : 0;

                    return new FlightDemandDto(flightNumber, bookings, Math.round(occupancy * 100) / 100.0);
                })
                .toList();

        Map<String, List<FlightDemandDto>> result = new HashMap<>();
        result.put("mostBooked",
                sortedTopN(demandStats, Comparator.comparingLong(FlightDemandDto::getBookings).reversed(), 5));
        result.put("leastBooked", sortedTopN(demandStats, Comparator.comparingLong(FlightDemandDto::getBookings), 5));
        return result;
    }

    // ── 4. Peak Travel Analytics (by day of week) ──
    @GetMapping("/peak-travel")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> peakTravelAnalytics() {
        List<Object[]> raw = reservationRepository.countBookingsByDayOfWeek();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("day", row[0]);
            entry.put("bookings", row[1]);
            result.add(entry);
        }
        return result;
    }

    // ── 5. Top Customers ──
    @GetMapping("/top-customers")
    @Transactional(readOnly = true)
    public List<TopCustomerDto> topCustomers() {
        List<Reservation> all = reservationRepository.findAll();
        all.forEach(this::forceLoad);

        Map<String, List<Reservation>> byUser = all.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getEmail()));

        return byUser.entrySet().stream()
                .map(entry -> {
                    List<Reservation> userReservations = entry.getValue();
                    String name = userReservations.get(0).getUser().getName();
                    String email = entry.getKey();
                    long totalBookings = userReservations.size();
                    BigDecimal totalSpent = userReservations.stream()
                            .map(Reservation::getTotalPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TopCustomerDto(name, email, totalBookings, totalSpent);
                })
                .sorted(Comparator.comparing(TopCustomerDto::getTotalSpent).reversed())
                .limit(10)
                .toList();
    }

    // ── 6. Business Insights (single cards) ──
    @GetMapping("/insights")
    @Transactional(readOnly = true)
    public Map<String, Object> businessInsights() {
        Map<String, List<RouteAnalyticsDto>> routes = routeAnalytics();
        Map<String, List<FlightDemandDto>> demand = flightDemandAnalytics();

        Map<String, Object> insights = new HashMap<>();

        insights.put("mostProfitableRoute",
                routes.get("highestRevenue").isEmpty() ? null : routes.get("highestRevenue").get(0));

        insights.put("highestDemandFlight",
                demand.get("mostBooked").isEmpty() ? null : demand.get("mostBooked").get(0));

        List<FlightDemandDto> byOccupancyAsc = demand.values().stream()
                .flatMap(List::stream)
                .distinct()
                .sorted(Comparator.comparingDouble(FlightDemandDto::getOccupancyPercent))
                .toList();
        insights.put("lowestOccupancyRoute", byOccupancyAsc.isEmpty() ? null : byOccupancyAsc.get(0));

        insights.put("highestCancellationRoute",
                routes.get("mostCancelled").isEmpty() ? null : routes.get("mostCancelled").get(0));

        return insights;
    }

    private <T> List<T> sortedTopN(List<T> list, Comparator<T> comparator, int n) {
        return list.stream().sorted(comparator).limit(n).toList();
    }

    private void forceLoad(Reservation r) {
        r.getUser().getEmail();
        r.getFlight().getSource();
        r.getPassengers().forEach(Passenger::getStatus);
    }
}