package com.pagatu.coffee.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDto {

    private Long id;
    private Long userId;
    private String username;
    private Double amount;
    private String description;
    private LocalDateTime paymentDate;
    private Long groupId;
    private String groupName;
}
