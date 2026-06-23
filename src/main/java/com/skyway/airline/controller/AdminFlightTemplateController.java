package com.skyway.airline.controller;

import com.skyway.airline.dto.FlightTemplateRequest;
import com.skyway.airline.entity.FlightTemplate;
import com.skyway.airline.repository.FlightTemplateRepository;
import com.skyway.airline.service.FlightGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/flight-templates")
@RequiredArgsConstructor
public class AdminFlightTemplateController {

    private final FlightTemplateRepository flightTemplateRepository;
    private final FlightGenerationService flightGenerationService;

    @GetMapping
    public List<FlightTemplate> getAll() {
        return flightTemplateRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FlightTemplateRequest req) {
        FlightTemplate template = FlightTemplate.builder()
                .airlineCode(req.getAirlineCode())
                .airlineName(req.getAirlineName())
                .flightNumber(req.getFlightNumber())
                .source(req.getSource())
                .destination(req.getDestination())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .duration(req.getDuration())
                .aircraft(req.getAircraft())
                .basePrice(req.getBasePrice())
                .totalSeats(req.getTotalSeats())
                .frequency(req.getFrequency())
                .active(true)
                .build();

        FlightTemplate saved = flightTemplateRepository.save(template);

        // Generate immediately so the admin sees results right away
        FlightGenerationService.GenerationResult result = flightGenerationService.runGeneration();

        return ResponseEntity.ok(Map.of(
                "template", saved,
                "flightsCreated", result.created(),
                "flightsSkipped", result.skipped()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FlightTemplateRequest req) {
        FlightTemplate template = flightTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setAirlineCode(req.getAirlineCode());
        template.setAirlineName(req.getAirlineName());
        template.setFlightNumber(req.getFlightNumber());
        template.setSource(req.getSource());
        template.setDestination(req.getDestination());
        template.setDepartureTime(req.getDepartureTime());
        template.setArrivalTime(req.getArrivalTime());
        template.setDuration(req.getDuration());
        template.setAircraft(req.getAircraft());
        template.setBasePrice(req.getBasePrice());
        template.setTotalSeats(req.getTotalSeats());
        template.setFrequency(req.getFrequency());

        return ResponseEntity.ok(flightTemplateRepository.save(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!flightTemplateRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Template not found"));
        }
        flightTemplateRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Template deleted successfully"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        FlightTemplate template = flightTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setActive(true);
        FlightTemplate saved = flightTemplateRepository.save(template);
        FlightGenerationService.GenerationResult result = flightGenerationService.runGeneration();
        return ResponseEntity.ok(Map.of("template", saved, "flightsCreated", result.created()));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        FlightTemplate template = flightTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setActive(false);
        return ResponseEntity.ok(flightTemplateRepository.save(template));
    }

    @PostMapping("/generate-now")
    public ResponseEntity<?> generateNow() {
        FlightGenerationService.GenerationResult result = flightGenerationService.runGeneration();
        return ResponseEntity.ok(Map.of(
                "flightsCreated", result.created(),
                "flightsSkipped", result.skipped()));
    }
}