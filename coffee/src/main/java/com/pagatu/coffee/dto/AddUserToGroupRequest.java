package com.pagatu.coffee.dto;

import com.pagatu.coffee.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUserToGroupRequest {
    private Long userId;
    private Long groupId;
    private Status status = Status.NON_PAGATO; // Default status
    private Boolean isAdmin;
}
