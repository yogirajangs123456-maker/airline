package com.skyway.airline.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FlightDemandDto {
    private String flightNumber;
    private long bookings;
    private double occupancyPercent;
}