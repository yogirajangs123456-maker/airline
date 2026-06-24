package com.skyway.airline.repository;

import com.skyway.airline.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findBySourceIgnoreCaseAndDestinationIgnoreCaseAndJourneyDateAndActiveTrue(
            String source, String destination, LocalDate journeyDate);

    List<Flight> findBySourceIgnoreCaseContainingAndDestinationIgnoreCaseContainingAndActiveTrue(
            String source, String destination);

    @Query(value = "SELECT DISTINCT source FROM flights WHERE active = true " +
            "UNION " +
            "SELECT DISTINCT destination FROM flights WHERE active = true", nativeQuery = true)
    List<String> findDistinctCities();

    @Query("SELECT f FROM Flight f WHERE " +
            "(:source IS NULL OR LOWER(f.source) LIKE LOWER(CONCAT('%', :source, '%'))) AND " +
            "(:destination IS NULL OR LOWER(f.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) AND " +
            "(:date IS NULL OR f.journeyDate = :date) AND " +
            "(:flightNumber IS NULL OR LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :flightNumber, '%'))) " +
            "ORDER BY f.journeyDate DESC, f.departureTime ASC")
    List<Flight> adminSearch(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("date") LocalDate date,
            @Param("flightNumber") String flightNumber);

    boolean existsByFlightNumberAndJourneyDate(String flightNumber, LocalDate journeyDate);

    List<Flight> findByTemplateIdIsNotNullOrderByJourneyDateDesc();

    long countByTemplateIdIsNotNull();

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.templateId IS NOT NULL AND f.journeyDate = :today")
    long countGeneratedToday(@Param("today") LocalDate today);

    // NEW — find the latest generated date for a specific template, so the
    // scheduler only fills the GAP instead of regenerating the whole window
    @Query("SELECT MAX(f.journeyDate) FROM Flight f WHERE f.templateId = :templateId")
    LocalDate findLatestGeneratedDateForTemplate(@Param("templateId") Long templateId);

    // NEW — all flights (both manual and auto), for the unified "Show All Flights" page
    @Query("SELECT f FROM Flight f WHERE " +
            "(:source IS NULL OR LOWER(f.source) LIKE LOWER(CONCAT('%', :source, '%'))) AND " +
            "(:destination IS NULL OR LOWER(f.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) AND " +
            "(:date IS NULL OR f.journeyDate = :date) AND " +
            "(:flightNumber IS NULL OR LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :flightNumber, '%'))) AND " +
            "(:source_type IS NULL OR " +
            "  (:source_type = 'AUTO' AND f.templateId IS NOT NULL) OR " +
            "  (:source_type = 'MANUAL' AND f.templateId IS NULL)) " +
            "ORDER BY f.journeyDate DESC, f.departureTime ASC")
    List<Flight> unifiedSearch(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("date") LocalDate date,
            @Param("flightNumber") String flightNumber,
            @Param("source_type") String sourceType);
}