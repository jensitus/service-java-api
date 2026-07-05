package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SupportResistanceDto {
    private String coinId;
    private String coinName;
    private BigDecimal currentPrice;
    private List<BigDecimal> resistanceLevels;
    private List<BigDecimal> supportLevels;
    private BigDecimal nearestResistance;
    private BigDecimal nearestSupport;
    private int dataPoints;
}
