package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionDto {

    private String type;
    private String groupName;
    private String groupDescription;
    private Long invitationId;
    private String invitedBy;
    private String message;
}