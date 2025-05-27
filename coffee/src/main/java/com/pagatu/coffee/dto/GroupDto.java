package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.Utente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupDto {
    private String name;
    private List<Utente> users;
}
