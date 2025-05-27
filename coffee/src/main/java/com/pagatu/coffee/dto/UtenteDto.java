package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtenteDto {

    private Long id;
    private Long authId;
    private String username;
    private String email;
    private String name;
    private String lastname;
    private Status status;

    // Manteniamo List<String> per semplicità nel trasferimento dati
    // La conversione a List<Group> avviene nel service
    private Object groups; // Flessibile per gestire diversi formati

    // Metodi helper per gestire i gruppi
    @SuppressWarnings("unchecked")
    public List<String> getGroupsAsStringList() {
        if (groups instanceof List<?>) {
            try {
                return (List<String>) groups;
            } catch (ClassCastException e) {
                // Handle mixed types in list
                return ((List<?>) groups).stream()
                        .map(Object::toString)
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .toList();
            }
        }
        return List.of();
    }
}