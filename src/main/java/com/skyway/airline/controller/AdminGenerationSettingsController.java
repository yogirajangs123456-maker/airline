package com.skyway.airline.controller;

import com.skyway.airline.entity.GenerationSettings;
import com.skyway.airline.service.FlightGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/generation-settings")
@RequiredArgsConstructor
public class AdminGenerationSettingsController {

    private final FlightGenerationService flightGenerationService;

    @GetMapping
    public Map<String, Object> getSettings() {
        return Map.of("windowDays", flightGenerationService.getCurrentWindowDays());
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Integer> body) {
        try {
            GenerationSettings updated = flightGenerationService.updateWindowDays(body.get("windowDays"));
            FlightGenerationService.GenerationResult result = flightGenerationService.runGeneration();
            return ResponseEntity.ok(Map.of(
                    "windowDays", updated.getWindowDays(),
                    "flightsCreated", result.created()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}