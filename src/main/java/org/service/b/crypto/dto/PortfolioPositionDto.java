package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PortfolioPositionDto {
    private String coinId;
    private String coinName;
    private BigDecimal netQuantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal currentPriceEur;
    private BigDecimal currentValue;
    private BigDecimal currentValueEur;
    private BigDecimal totalInvested;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlPct;
    private BigDecimal unrealizedPnlEur;
    private BigDecimal unrealizedPnlPctEur;
    private BigDecimal totalFees;
}
