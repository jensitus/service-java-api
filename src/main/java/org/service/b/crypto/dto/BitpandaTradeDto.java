package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BitpandaTradeDto {
    private String bitpandaId;
    private String type;          // buy / sell
    private String coinSymbol;    // BTC, LTC, ETH, XRP, ...
    private String mappedCoinId;  // bitcoin, litecoin, ... (null if not tracked)
    private BigDecimal cryptoAmount;
    private BigDecimal fiatAmount;
    private BigDecimal price;
    private String status;
    private LocalDateTime tradedAt;
    private boolean tracked;      // whether this coin maps to one we support
}
