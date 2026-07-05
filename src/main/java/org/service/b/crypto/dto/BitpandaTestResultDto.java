package org.service.b.crypto.dto;

import lombok.Data;

import java.util.List;

@Data
public class BitpandaTestResultDto {
    private boolean success;
    private String message;
    private int totalCount;      // total trades reported by Bitpanda
    private List<BitpandaTradeDto> trades = List.of();
}
