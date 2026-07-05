package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IndicatorDto {
    private String coinId;
    private String coinName;
    private BigDecimal currentPrice;
    private BigDecimal currentPriceEur;
    private BigDecimal rsi14;
    private BigDecimal ma7;
    private BigDecimal ma14;
    private BigDecimal ma30;
    private BigDecimal ma7Eur;
    private BigDecimal ma14Eur;
    private BigDecimal ma30Eur;
    private String rsiSignal;   // OVERSOLD, OVERBOUGHT, NEUTRAL
    private String maSignal;    // BULLISH, BEARISH, NEUTRAL
    private int dataPoints;
}
