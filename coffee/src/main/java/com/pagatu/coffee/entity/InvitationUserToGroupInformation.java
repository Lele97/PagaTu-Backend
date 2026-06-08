package com.pagatu.coffee.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "invitation_user_to_group_information")
public class InvitationUserToGroupInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private Long userWhoSentInvitation;
    private String groupName;
    private String user;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;

    @Column(name = "expired_date")
    private LocalDateTime expiredDate;

    private InvitationStatus invitationStatus;
}
