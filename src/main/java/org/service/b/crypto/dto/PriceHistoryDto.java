package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceHistoryDto {
    private LocalDateTime recordedAt;
    private BigDecimal priceUsd;
    private BigDecimal priceEur;
}
