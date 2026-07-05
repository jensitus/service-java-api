package org.service.b.crypto.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertForm {

    @NotNull
    private String coinId;

    @NotNull
    @Positive
    private BigDecimal targetPrice;

    @NotNull
    private String condition; // ABOVE or BELOW

}
