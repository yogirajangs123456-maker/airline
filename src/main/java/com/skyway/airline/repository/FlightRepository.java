package com.skyway.airline.repository;

import com.skyway.airline.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

        // Bug #9 cascades: was "IsActiveTrue" — still works since the field is now
        // "active"
        // and Spring Data derives isActive() from the boolean field correctly
        List<Flight> findBySourceIgnoreCaseAndDestinationIgnoreCaseAndJourneyDateAndActiveTrue(
                        String source, String destination, LocalDate journeyDate);

        List<Flight> findBySourceIgnoreCaseContainingAndDestinationIgnoreCaseContainingAndActiveTrue(
                        String source, String destination);

        @Query(value = "SELECT DISTINCT source FROM flights WHERE active = true " +
                        "UNION " +
                        "SELECT DISTINCT destination FROM flights WHERE active = true", nativeQuery = true)
        List<String> findDistinctCities();
}