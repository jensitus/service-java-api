package org.service.b.crypto.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BitpandaKeyForm {

    @NotBlank
    private String apiKey;

}
