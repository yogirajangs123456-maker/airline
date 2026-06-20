package com.skyway.airline.dto;

import lombok.Data;
import java.util.List;

@Data
public class RefundPreviewRequest {
    private String pnr;
    private List<Long> passengerIds;
}