package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembershipDto {

    private Long groupId;
    private String groupName;
    private PaymentStatus status;
    private Boolean isAdmin;
    private java.time.LocalDateTime joinedAt;
}
