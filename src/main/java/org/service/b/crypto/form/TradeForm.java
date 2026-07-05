package org.service.b.crypto.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeForm {

    @NotNull
    private String coinId;

    @NotNull
    private String side; // BUY or SELL

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    @Positive
    private BigDecimal pricePerUnit;

    private BigDecimal fee;

    private String currency = "EUR";

    @NotNull
    private LocalDateTime tradedAt;

    private String note;

}
