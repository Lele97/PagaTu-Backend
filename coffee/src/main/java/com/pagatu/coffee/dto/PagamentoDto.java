package com.pagatu.coffee.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PagamentoDto {
    private Long id;
    private Long userId;
    private String username;
    private Double importo;
    private String descrizione;
    private LocalDateTime dataPagamento;
    private Long groupId;
    private String groupName;
}