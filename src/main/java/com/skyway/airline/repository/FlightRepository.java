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
                        "(:destination IS NULL OR LOWER(f.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) AND "
                        +
                        "(:date IS NULL OR f.journeyDate = :date) AND " +
                        "(:flightNumber IS NULL OR LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :flightNumber, '%'))) "
                        +
                        "ORDER BY f.journeyDate DESC, f.departureTime ASC")
        List<Flight> adminSearch(
                        @Param("source") String source,
                        @Param("destination") String destination,
                        @Param("date") LocalDate date,
                        @Param("flightNumber") String flightNumber);

        // NEW — for duplicate prevention during generation
        boolean existsByFlightNumberAndJourneyDate(String flightNumber, LocalDate journeyDate);

        // NEW — for "Generated Flights" admin page (only template-generated ones)
        List<Flight> findByTemplateIdIsNotNullOrderByJourneyDateDesc();

        // NEW — for Automation Dashboard counts
        long countByTemplateIdIsNotNull();

        @Query("SELECT COUNT(f) FROM Flight f WHERE f.templateId IS NOT NULL AND f.journeyDate = :today")
        long countGeneratedToday(@Param("today") LocalDate today);
}