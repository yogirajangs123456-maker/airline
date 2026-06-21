package com.skyway.airline.controller;

import com.skyway.airline.dto.FlightRequest;
import com.skyway.airline.entity.Flight;
import com.skyway.airline.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/flights")
@RequiredArgsConstructor
public class AdminFlightController {

    private final FlightRepository flightRepository;

    @GetMapping("/search")
    public List<Flight> search(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String flightNumber) {

        LocalDate parsedDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        String src = (source != null && !source.isBlank()) ? source.trim() : null;
        String dest = (destination != null && !destination.isBlank()) ? destination.trim() : null;
        String fn = (flightNumber != null && !flightNumber.isBlank()) ? flightNumber.trim() : null;

        return flightRepository.adminSearch(src, dest, parsedDate, fn);
    }

    @GetMapping
    public List<Flight> getAll() {
        return flightRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FlightRequest req) {
        Flight flight = Flight.builder()
                .airlineCode(req.getAirlineCode())
                .airlineName(req.getAirlineName())
                .flightNumber(req.getFlightNumber())
                .source(req.getSource())
                .destination(req.getDestination())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .journeyDate(req.getJourneyDate())
                .price(req.getPrice())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats())
                .duration(req.getDuration())
                .active(true)
                .build();
        return ResponseEntity.ok(flightRepository.save(flight));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FlightRequest req) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        int bookedSeats = flight.getTotalSeats() - flight.getAvailableSeats();

        flight.setAirlineCode(req.getAirlineCode());
        flight.setAirlineName(req.getAirlineName());
        flight.setFlightNumber(req.getFlightNumber());
        flight.setSource(req.getSource());
        flight.setDestination(req.getDestination());
        flight.setDepartureTime(req.getDepartureTime());
        flight.setArrivalTime(req.getArrivalTime());
        flight.setJourneyDate(req.getJourneyDate());
        flight.setPrice(req.getPrice());
        flight.setTotalSeats(req.getTotalSeats());
        flight.setAvailableSeats(Math.max(0, req.getTotalSeats() - bookedSeats));
        flight.setDuration(req.getDuration());

        return ResponseEntity.ok(flightRepository.save(flight));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!flightRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Flight not found"));
        }
        flightRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Flight deleted successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        flight.setActive(false);
        return ResponseEntity.ok(flightRepository.save(flight));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        flight.setActive(true);
        return ResponseEntity.ok(flightRepository.save(flight));
    }
}