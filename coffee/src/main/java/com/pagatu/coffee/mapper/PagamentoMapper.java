package com.pagatu.coffee.mapper;

import com.pagatu.coffee.dto.PagamentoDto;
import com.pagatu.coffee.entity.Pagamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoMapper {

    public PagamentoDto toDto(Pagamento pagamento) {
        if (pagamento == null) {
            return null;
        }

        PagamentoDto dto = new PagamentoDto();
        dto.setId(pagamento.getId());

        // Ottieni lo userId e lo username dall'oggetto utente associato al pagamento
        if (pagamento.getUserGroupMembership() != null) {
            dto.setUserId(pagamento.getUserGroupMembership().getId());
            dto.setUsername(pagamento.getUserGroupMembership().getUtente().getUsername());
        }

        dto.setDataPagamento(pagamento.getDataPagamento());
        dto.setImporto(pagamento.getImporto());
        dto.setDescrizione(pagamento.getDescrizione());

        return dto;
    }

    public Pagamento toEntity(PagamentoDto dto) {
        if (dto == null) {
            return null;
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setId(dto.getId());
        // Non impostiamo direttamente l'utente qui perché abbiamo bisogno di un riferimento all'entità Utente
        // Questo dovrebbe essere gestito dal servizio che utilizza il mapper
        pagamento.setDataPagamento(dto.getDataPagamento());
        pagamento.setImporto(dto.getImporto());
        pagamento.setDescrizione(dto.getDescrizione());

        return pagamento;
    }
}