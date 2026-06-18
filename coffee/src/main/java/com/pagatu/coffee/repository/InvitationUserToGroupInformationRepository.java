package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.InvitationUserToGroupInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing group invitation records.
 */
public interface InvitationUserToGroupInformationRepository extends JpaRepository<InvitationUserToGroupInformation, Long> {

    /**
     * Finds an invitation by ID only if it is still active.
     *
     * @param id the invitation identifier
     * @return the active invitation, if present
     */
    @Query("SELECT i FROM InvitationUserToGroupInformation i WHERE i.id = :id AND i.invitationStatus = com.pagatu.coffee.entity.InvitationStatus.ACTIVE")
    Optional<InvitationUserToGroupInformation> findByIdWithStatusActive(@Param("id") Long id);

    @Query("SELECT i FROM InvitationUserToGroupInformation i WHERE i.invitationStatus = com.pagatu.coffee.entity.InvitationStatus.ACTIVE " +
            "AND i.expiredDate > :now AND (i.username = :username OR i.email = :email)")
    List<InvitationUserToGroupInformation> findActiveInvitationsForUser(
            @Param("username") String username,
            @Param("email") String email,
            @Param("now") LocalDateTime now);
}
