package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PortfolioSummaryDto {
    private List<PortfolioPositionDto> positions;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalCurrentValueEur;
    private BigDecimal totalUnrealizedPnl;
    private BigDecimal totalUnrealizedPnlPct;
    private BigDecimal totalUnrealizedPnlEur;
    private BigDecimal totalUnrealizedPnlPctEur;
}
