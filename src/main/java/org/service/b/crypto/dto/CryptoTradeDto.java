package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CryptoTradeDto {
    private Long id;
    private String coinId;
    private String side;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal fee;
    private BigDecimal totalValue;
    private LocalDateTime tradedAt;
    private String note;
    private String currency;
    private String source;
    private BigDecimal currentPriceUsd;
    private BigDecimal currentPriceEur;
    private BigDecimal unrealizedPnl;
}
