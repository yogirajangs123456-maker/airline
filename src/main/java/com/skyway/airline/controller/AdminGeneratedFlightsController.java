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
@RequestMapping("/api/admin/all-flights")
@RequiredArgsConstructor
public class AdminGeneratedFlightsController {

    private final FlightRepository flightRepository;
    private final FlightTemplateRepository flightTemplateRepository;

    @GetMapping
    public List<Flight> getAll() {
        return flightRepository.findAll();
    }

    @GetMapping("/search")
    public List<Flight> search(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) String sourceType) {

        LocalDate parsedDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;

        return flightRepository.unifiedSearch(
                blankToNull(source), blankToNull(destination), parsedDate,
                blankToNull(flightNumber), blankToNull(sourceType));
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("activeTemplates", flightTemplateRepository.findByStatus("ACTIVE").size());
        stats.put("totalGeneratedFlights", flightRepository.countByTemplateIdIsNotNull());
        stats.put("generatedToday", flightRepository.countGeneratedToday(LocalDate.now()));

        List<Flight> all = flightRepository.findAll();

        long upcoming = all.stream().filter(Flight::isBookable).count();
        long completed = all.stream().filter(f -> "COMPLETED".equals(f.getStatus())).count();
        long cancelled = all.stream().filter(f -> "CANCELLED".equals(f.getStatus())).count();
        long departed = all.stream().filter(f -> "DEPARTED".equals(f.getStatus())).count();
        long manual = all.stream().filter(f -> f.getTemplateId() == null).count();
        long auto = all.stream().filter(f -> f.getTemplateId() != null).count();

        stats.put("upcomingFlights", upcoming);
        stats.put("departedFlights", departed);
        stats.put("completedFlights", completed);
        stats.put("cancelledFlights", cancelled);
        stats.put("manualFlights", manual);
        stats.put("autoFlights", auto);

        return stats;
    }

    private String blankToNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }
}