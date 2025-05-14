package com.pagatu.coffee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NuovoPagamentoRequest {

    @NotNull
    @Positive
    private Double importo;

    private String descrizione;
}