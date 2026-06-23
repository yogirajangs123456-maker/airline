package com.skyway.airline.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class FlightTemplateRequest {
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
}