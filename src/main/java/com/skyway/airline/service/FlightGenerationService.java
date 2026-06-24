package com.skyway.airline.service;

import com.skyway.airline.entity.Flight;
import com.skyway.airline.entity.FlightTemplate;
import com.skyway.airline.entity.GenerationSettings;
import com.skyway.airline.repository.FlightRepository;
import com.skyway.airline.repository.FlightTemplateRepository;
import com.skyway.airline.repository.GenerationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlightGenerationService {

    private final FlightTemplateRepository flightTemplateRepository;
    private final FlightRepository flightRepository;
    private final GenerationSettingsRepository generationSettingsRepository;

    private static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static final Set<DayOfWeek> WEEKENDS = Set.of(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    /** Runs every day at 12:00 AM server time, per spec. */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void scheduledGeneration() {
        runGeneration();
    }

    /** Also runs on every app startup/redeploy. */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        runGeneration();
    }

    @Transactional
    public GenerationResult runGeneration() {
        int windowDays = getCurrentWindowDays();
        List<FlightTemplate> templates = flightTemplateRepository.findByStatus("ACTIVE");

        LocalDate today = LocalDate.now();
        LocalDate desiredWindowEnd = today.plusDays(windowDays);

        int created = 0;
        int skipped = 0;

        for (FlightTemplate template : templates) {
            LocalDate latestExisting = flightRepository
                    .findLatestGeneratedDateForTemplate(template.getTemplateId());

            // Start filling from the day after the latest existing flight,
            // or from today if none exist yet.
            LocalDate fillFrom = (latestExisting != null && latestExisting.isAfter(today))
                    ? latestExisting.plusDays(1)
                    : today;

            for (LocalDate date = fillFrom; !date.isAfter(desiredWindowEnd); date = date.plusDays(1)) {
                if (!matchesFrequency(template.getFrequency(), date)) {
                    continue;
                }
                boolean exists = flightRepository
                        .existsByFlightNumberAndJourneyDate(template.getFlightNumber(), date);
                if (exists) {
                    skipped++;
                    continue;
                }
                createFlightInstance(template, date);
                created++;
            }
        }
        return new GenerationResult(created, skipped, windowDays);
    }

    public int getCurrentWindowDays() {
        List<GenerationSettings> all = generationSettingsRepository.findAll();
        if (all.isEmpty()) {
            GenerationSettings defaultSettings = GenerationSettings.builder().windowDays(10).build();
            generationSettingsRepository.save(defaultSettings);
            return 10;
        }
        return all.get(0).getWindowDays();
    }

    @Transactional
    public GenerationSettings updateWindowDays(int days) {
        if (days != 5 && days != 10 && days != 15 && days != 30) {
            throw new RuntimeException("Window must be one of: 5, 10, 15, 30 days.");
        }
        List<GenerationSettings> all = generationSettingsRepository.findAll();
        GenerationSettings settings = all.isEmpty()
                ? GenerationSettings.builder().windowDays(days).build()
                : all.get(0);
        settings.setWindowDays(days);
        return generationSettingsRepository.save(settings);
    }

    private boolean matchesFrequency(String frequency, LocalDate date) {
        if (frequency == null || frequency.isBlank()) {
            return false;
        }
        String trimmed = frequency.trim().toUpperCase();
        DayOfWeek dow = date.getDayOfWeek();

        if ("DAILY".equals(trimmed))
            return true;
        if ("WEEKDAYS".equals(trimmed))
            return WEEKDAYS.contains(dow);
        if ("WEEKENDS".equals(trimmed))
            return WEEKENDS.contains(dow);

        String dayAbbrev = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String[] allowedDays = trimmed.split(",");
        for (String d : allowedDays) {
            if (d.trim().equals(dayAbbrev)) {
                return true;
            }
        }
        return false;
    }

    private void createFlightInstance(FlightTemplate template, LocalDate date) {
        String durationStr = template.getDuration() != null
                ? template.getDuration()
                : formatDuration(template.getDepartureTime(), template.getArrivalTime());

        Flight flight = Flight.builder()
                .templateId(template.getTemplateId())
                .airlineCode(template.getAirlineCode())
                .airlineName(template.getAirlineName())
                .flightNumber(template.getFlightNumber())
                .source(template.getSource())
                .destination(template.getDestination())
                .departureTime(template.getDepartureTime())
                .arrivalTime(template.getArrivalTime())
                .journeyDate(date)
                .price(template.getBasePrice())
                .totalSeats(template.getTotalSeats())
                .availableSeats(template.getTotalSeats())
                .duration(durationStr)
                .active(true)
                .build();

        flightRepository.save(flight);
    }

    private String formatDuration(LocalTime dep, LocalTime arr) {
        Duration d = Duration.between(dep, arr);
        if (d.isNegative())
            d = d.plusDays(1);
        long hours = d.toHours();
        long minutes = d.toMinutes() % 60;
        return hours + "h " + minutes + "m";
    }

    public record GenerationResult(int created, int skipped, int windowDays) {
    }
}