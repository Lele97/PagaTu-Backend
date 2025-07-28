package com.pagatu.coffee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NuovoPagamentoPerRequest {

    private Long userId;

    @NotNull
    @Positive
    private Double importo;

    private String descrizione;
}
