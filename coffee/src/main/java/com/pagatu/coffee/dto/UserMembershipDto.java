package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserMembershipDto {

    private Long userId;
    private String username;
    private String name;
    private String lastname;
    private PaymentStatus status;
    private Boolean myTurn;
    private Boolean isAdmin;
    private java.time.LocalDateTime joinedAt;
}
