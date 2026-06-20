package com.skyway.airline.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BookingRequest {
    private Long flightId;
    private List<PassengerSeatRequest> passengers;
    private BigDecimal totalPrice;

    @Data
    public static class PassengerSeatRequest {
        private String passengerName;
        private String seatNumber;
    }
}