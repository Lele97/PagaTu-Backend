package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NuovoGruppoRequest {
    private String name;
    private String description;
    private List<Utente> users;
}
