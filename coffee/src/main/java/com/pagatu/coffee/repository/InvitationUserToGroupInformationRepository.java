package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.InvitationUserToGroupInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvitationUserToGroupInformationRepository extends JpaRepository<InvitationUserToGroupInformation, Long> {

    @Query("Select I from  InvitationUserToGroupInformation I Where I.userWhoSentInvitation =: userWhoSentInvitation AND I.groupName =: groupName AND I.user=: user AND I.invitationStatus='ACTIVE' ORDER BY i.createdAt DESC")
    InvitationUserToGroupInformation getInvitationUserToGroupInformationByParameter(@Param("userWhoSentInvitation") Long userWhoSentInvitation, @Param("groupName") String groupName, @Param("user") String user);

    @Query("SELECT I FROM InvitationUserToGroupInformation I where I.userWhoSentInvitation=: id AND I.invitationStatus='ACTIVE'")
    Optional<InvitationUserToGroupInformation> findByIdWithStatusActive(@Param("id") Long id);
}
