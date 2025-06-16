package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaltaPagamentoEvent {
    private Long prossimoUserId;
    private String prossimoUsername;
    private String prossimoEmail;
}
