package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProssimoPagamentoEvent {

    private Long ultimoPagamentoId;
    private String ultimoPagatoreUsername;
    private String ultimoPagatoreEmail;
    private Long prossimoUserId;
    private String prossimoUsername;
    private String prossimoEmail;
    private LocalDateTime dataUltimoPagamento;
    private Double importo;
    private String groupName;
}
