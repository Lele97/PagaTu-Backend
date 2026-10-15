package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NATS payload for a group invitation email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationEvent {

    private String email;
    private String username;
    private String groupName;
    private String userWhoSentTheInvitation;
    private String invitationId;
}
