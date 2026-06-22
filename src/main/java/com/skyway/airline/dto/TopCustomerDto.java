package com.skyway.airline.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TopCustomerDto {
    private String name;
    private String email;
    private long totalBookings;
    private BigDecimal totalSpent;
}