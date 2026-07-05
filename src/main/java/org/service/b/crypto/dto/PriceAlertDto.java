package org.service.b.crypto.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceAlertDto {
    private Long id;
    private String coinId;
    private BigDecimal targetPrice;
    private String condition;
    private Boolean active;
    private Boolean triggered;
    private LocalDateTime createdAt;
}
