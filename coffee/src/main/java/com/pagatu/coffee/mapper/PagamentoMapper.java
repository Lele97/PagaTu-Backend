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
        dto.setUserId(pagamento.getUserId());
        dto.setUsername(pagamento.getUsername());
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
        pagamento.setUserId(dto.getUserId());
        pagamento.setUsername(dto.getUsername());
        pagamento.setDataPagamento(dto.getDataPagamento());
        pagamento.setImporto(dto.getImporto());
        pagamento.setDescrizione(dto.getDescrizione());

        return pagamento;
    }
}
