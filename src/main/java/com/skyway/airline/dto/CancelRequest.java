package com.skyway.airline.dto;

import lombok.Data;

@Data
public class CancelRequest {
    private String pnr;
    private String otp;
}