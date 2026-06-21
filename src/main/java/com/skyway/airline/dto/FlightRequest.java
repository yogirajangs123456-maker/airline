package com.skyway.airline.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FlightRequest {
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
    private String duration;
}