package com.pagatu.coffee.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCoffeeRoundRequest {   private Long groupId;
    private Long payerId;
    private Double amount;
    private String notes;
}
