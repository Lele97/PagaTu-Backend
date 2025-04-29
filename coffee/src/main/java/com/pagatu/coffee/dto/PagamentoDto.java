package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoDto {
    private Long id;
    private Long userId;
    private String username;
    private LocalDateTime dataPagamento;
    private Double importo;
    private String descrizione;
}
