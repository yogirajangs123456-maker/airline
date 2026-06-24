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

    private String frequency;

    /**
     * ACTIVE → generate flights
     * SUSPENDED → stop future generation, keep existing flights
     * INACTIVE → same behavior as SUSPENDED for generation purposes;
     * kept as a distinct value for admin clarity (soft-deleted/retired)
     */
    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    public boolean isGenerationEligible() {
        return "ACTIVE".equals(status);
    }
}