package com.pagatu.coffee.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a pending invitation for a user to join a coffee payment group.
 * <p>
 * Invitations are created by group admins, expire after a configured time window,
 * and transition to {@link InvitationStatus#ACCEPTED} or {@link InvitationStatus#REJECTED}
 * once processed.
 * </p>
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "invitation_user_to_group_information")
public class InvitationUserToGroupInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** Primary key. */
    @Column(name = "id")
    private Long id;

    /** Auth ID of the admin who created the invitation. */
    @Column(name = "user_who_sent_invitation")
    private Long userWhoSentInvitation;

    /** Target group name. */
    @Column(name = "group_name")
    private String groupName;

    /** Username of the invited user. */
    @Column(name = "username")
    private String username;

    /** Email address used for the invitation notification. */
    @Column(name = "email")
    private String email;

    /** Timestamp when the invitation was created. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Timestamp when the invitation was accepted or rejected. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Expiration timestamp for the invitation link. */
    @Column(name = "expired_date")
    private LocalDateTime expiredDate;

    /** Current invitation lifecycle status. */
    @Column(name = "invitation_status")
    private InvitationStatus invitationStatus;
}
