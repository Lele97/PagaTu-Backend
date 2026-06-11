package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NATS payload for an invitation accept/reject notification email.
 */
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
