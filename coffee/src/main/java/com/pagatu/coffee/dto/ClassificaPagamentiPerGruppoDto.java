package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassificaPagamentiPerGruppoDto {

    private String username;
    private double totaleImporto;
    private long totalePagamenti;
}
