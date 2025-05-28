package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProssimoPagamentoDto {
    private Long userId;
    private String username;
    private String email;
    private Long groupId;      // Added group information
    private String groupName;  // Added group information
}