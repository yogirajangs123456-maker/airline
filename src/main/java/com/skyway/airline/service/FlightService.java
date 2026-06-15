package com.skyway.airline.service;

import com.skyway.airline.entity.Flight;
import com.skyway.airline.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    /**
     * Search flights by source, destination, and optional date.
     * Matches are case-insensitive. If no date given, returns all active matches.
     */
    public List<Flight> searchFlights(String source, String destination, String date) {
        if (date != null && !date.isBlank()) {
            LocalDate journeyDate = LocalDate.parse(date);
            return flightRepository
                    .findBySourceIgnoreCaseAndDestinationIgnoreCaseAndJourneyDateAndActiveTrue(
                            source, destination, journeyDate);
        }
        return flightRepository
                .findBySourceIgnoreCaseContainingAndDestinationIgnoreCaseContainingAndActiveTrue(
                        source, destination);
    }

    /**
     * Returns a single flight by ID, or throws if not found or inactive.
     */
    public Flight getFlightById(Long flightId) {
        return flightRepository.findById(flightId)
                .filter(Flight::isActive)
                .orElseThrow(() -> new RuntimeException("Flight not found or no longer active"));
    }

    /**
     * Returns all currently active flights (admin / browse all use-case).
     */
    public List<Flight> getAllActiveFlights() {
        return flightRepository.findAll()
                .stream()
                .filter(Flight::isActive)
                .toList();
    }

    /**
     * Deactivates a flight (soft-delete). Does not physically remove from DB.
     */
    @Transactional
    public Flight deactivateFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        flight.setActive(false);
        return flightRepository.save(flight);
    }

    /**
     * Updates available seat count — called after booking or cancellation.
     */
    @Transactional
    public void adjustAvailableSeats(Long flightId, int delta) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        int updated = flight.getAvailableSeats() + delta;
        if (updated < 0)
            throw new RuntimeException("No available seats on this flight");
        if (updated > flight.getTotalSeats())
            updated = flight.getTotalSeats();
        flight.setAvailableSeats(updated);
        flightRepository.save(flight);
    }
}