package com.skyway.airline.controller;

import com.skyway.airline.entity.Flight;
import com.skyway.airline.repository.FlightRepository;
import com.skyway.airline.repository.FlightTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/generated-flights")
@RequiredArgsConstructor
public class AdminGeneratedFlightsController {

    private final FlightRepository flightRepository;
    private final FlightTemplateRepository flightTemplateRepository;

    @GetMapping
    public List<Flight> getAll() {
        return flightRepository.findByTemplateIdIsNotNullOrderByJourneyDateDesc();
    }

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

        return flightRepository.adminSearch(src, dest, parsedDate, fn).stream()
                .filter(f -> f.getTemplateId() != null)
                .toList();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("activeTemplates", flightTemplateRepository.findByActiveTrue().size());
        stats.put("totalGeneratedFlights", flightRepository.countByTemplateIdIsNotNull());
        stats.put("generatedToday", flightRepository.countGeneratedToday(LocalDate.now()));

        List<Flight> generated = flightRepository.findByTemplateIdIsNotNullOrderByJourneyDateDesc();

        long upcoming = generated.stream().filter(Flight::isBookable).count();
        long completed = generated.stream().filter(f -> "COMPLETED".equals(f.getStatus())).count();
        long cancelled = generated.stream().filter(f -> "CANCELLED".equals(f.getStatus())).count();

        stats.put("upcomingFlights", upcoming);
        stats.put("completedFlights", completed);
        stats.put("cancelledFlights", cancelled);

        return stats;
    }
}