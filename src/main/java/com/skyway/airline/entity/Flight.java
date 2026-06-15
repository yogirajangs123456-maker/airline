package com.skyway.airline.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long flightId;

    private String airlineCode;
    private String airlineName;
    private String flightNumber;
    private String source;
    private String destination;

    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private LocalDate journeyDate;

    private BigDecimal price;
    private int totalSeats;
    private int availableSeats;
    private String duration;

    // Fix 1: renamed isActive → active so Lombok generates isActive()/setActive()
    // correctly
    // Fix 2: @Builder.Default so the builder honours the default value
    @Builder.Default
    private boolean active = true;
}