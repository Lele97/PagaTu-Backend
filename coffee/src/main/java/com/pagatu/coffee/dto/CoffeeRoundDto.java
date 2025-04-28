package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeRoundDto {
    private Long id;
    private Long groupId;
    private Long payerId;
    private String payerName;
    private LocalDateTime paymentDate;
    private Double amount;
    private String notes;
    private Long nextPayerId;
    private String nextPayerName;
    private LocalDateTime notificationSent;
}
