package com.skyway.airline.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RouteAnalyticsDto {
    private String route;
    private long bookings;
    private BigDecimal revenue;
    private long cancellations;
}