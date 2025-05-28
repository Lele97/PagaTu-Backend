package com.pagatu.coffee.entity;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NuovoPagamentoRequest {
    @Positive(message = "L'importo deve essere positivo")
    private Double importo;
    private String descrizione;
    private String gruppo;
}
