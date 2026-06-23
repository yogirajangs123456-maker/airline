package com.skyway.airline.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "flight_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    private String airlineCode;
    private String airlineName;
    private String flightNumber;
    private String source;
    private String destination;

    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String duration;
    private String aircraft;

    private BigDecimal basePrice;
    private int totalSeats;

    /**
     * Comma-separated days, e.g. "MON,WED,FRI", or the literal values
     * "DAILY", "WEEKDAYS", "WEEKENDS".
     */
    private String frequency;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}