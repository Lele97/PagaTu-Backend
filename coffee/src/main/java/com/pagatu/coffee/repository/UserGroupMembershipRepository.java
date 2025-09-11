package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Status;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, Long> {

    // Find all memberships for a group
    List<UserGroupMembership> findByGroup(Group group);

    // Find memberships by group and status
    List<UserGroupMembership> findByGroupAndStatus(Group group, Status status);

    @Query("select ugm from UserGroupMembership ugm join ugm.group ug where ug.name= :group and ugm.myTurn=true")
    UserGroupMembership findUserTurn(String group);

    // Check if user exists in group
    boolean existsByUtenteAndGroup(Utente utente, Group group);
}
