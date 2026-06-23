package com.skyway.airline.service;

import com.skyway.airline.entity.Flight;
import com.skyway.airline.entity.FlightTemplate;
import com.skyway.airline.repository.FlightRepository;
import com.skyway.airline.repository.FlightTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlightGenerationService {

    private final FlightTemplateRepository flightTemplateRepository;
    private final FlightRepository flightRepository;

    private static final int BOOKING_WINDOW_DAYS = 30;

    private static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static final Set<DayOfWeek> WEEKENDS = Set.of(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    /** Runs once daily at 1:00 AM server time, per spec. */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void generateUpcomingFlights() {
        runGeneration();
    }

    /**
     * Also runs once on every application startup/redeploy, so a newly
     * created template doesn't have to wait until 1 AM to produce flights.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        runGeneration();
    }

    @Transactional
    public GenerationResult runGeneration() {
        List<FlightTemplate> templates = flightTemplateRepository.findByActiveTrue();
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(BOOKING_WINDOW_DAYS);

        int created = 0;
        int skipped = 0;

        for (FlightTemplate template : templates) {
            for (LocalDate date = today; !date.isAfter(windowEnd); date = date.plusDays(1)) {
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
        return new GenerationResult(created, skipped);
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

    private String formatDuration(java.time.LocalTime dep, java.time.LocalTime arr) {
        Duration d = Duration.between(dep, arr);
        if (d.isNegative())
            d = d.plusDays(1); // handles overnight flights
        long hours = d.toHours();
        long minutes = d.toMinutes() % 60;
        return hours + "h " + minutes + "m";
    }

    public record GenerationResult(int created, int skipped) {
    }
}