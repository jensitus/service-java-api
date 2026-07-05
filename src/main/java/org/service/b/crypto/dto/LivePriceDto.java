package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LivePriceDto {
    private String coinId;
    private String coinName;
    private BigDecimal priceUsd;
    private BigDecimal priceChange24h;
    private BigDecimal marketCapUsd;
    private BigDecimal priceEur;
    private BigDecimal priceChange24hEur;
    private BigDecimal marketCapEur;
    private LocalDateTime fetchedAt;
}
