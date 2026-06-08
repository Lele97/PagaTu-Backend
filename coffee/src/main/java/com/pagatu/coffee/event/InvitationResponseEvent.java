package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponseEvent {
    private String username;
    private String email;
    private String groupName;
    private Boolean accepted;
    private Long userWhoSentTheInvitation;
}
