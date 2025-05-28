package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, Long> {

    // Find membership by user and group
    Optional<UserGroupMembership> findByUtenteAndGroup(Utente utente, Group group);

    // Find all memberships for a user
    List<UserGroupMembership> findByUtente(Utente utente);

    // Find all memberships for a group
    List<UserGroupMembership> findByGroup(Group group);

    // Find memberships by status
    List<UserGroupMembership> findByStatus(Status status);

    // Find memberships by group and status
    List<UserGroupMembership> findByGroupAndStatus(Group group, Status status);

    // Find admin memberships for a group
    @Query("SELECT m FROM UserGroupMembership m WHERE m.group = :group AND m.isAdmin = true")
    List<UserGroupMembership> findAdminsByGroup(@Param("group") Group group);

    // Check if user exists in group
    boolean existsByUtenteAndGroup(Utente utente, Group group);

    // Count users in group by status
    @Query("SELECT COUNT(m) FROM UserGroupMembership m WHERE m.group = :group AND m.status = :status")
    long countByGroupAndStatus(@Param("group") Group group, @Param("status") Status status);
}
