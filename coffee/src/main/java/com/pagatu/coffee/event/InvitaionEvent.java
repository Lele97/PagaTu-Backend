package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitaionEvent {

    private String email;
    private String username;
    private String groupName;
    private String userWhoSentTheInvitation;
}
