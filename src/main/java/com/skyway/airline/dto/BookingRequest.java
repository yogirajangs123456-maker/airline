package com.skyway.airline.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BookingRequest {
    private Long flightId;
    private String seatNumber;
    private String passengerName;
    private BigDecimal totalPrice;
}