package com.pagatu.coffee.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewPaymentRequest {

    @NotNull(message = "L'importo è obbligatorio")
    @Positive(message = "L'importo deve essere positivo")
    @JsonAlias({ "importo", "prezzo" })
    private Double amount;

    @JsonAlias({ "descrizione", "causale" })
    private String description;
}
