package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembershipDto {

    private Long groupId;
    private String groupName;
    private Status status;
    private Boolean isAdmin;
    private java.time.LocalDateTime joinedAt;
}
