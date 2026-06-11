package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when an invited user accepts or rejects a group invitation.
 * Consumed by the mail service to notify the group admin.
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
