package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.PaymentStatus;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.CoffeeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for UserGroupMembership entity operations.
 */
@Repository
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, Long> {

    @Query("select ugm from UserGroupMembership ugm join ugm.group ug where ug.name= :group and ugm.myTurn=true")
    UserGroupMembership findUserTurn(String group);

    List<UserGroupMembership> findByGroup(Group group);

    List<UserGroupMembership> findByGroupAndStatus(Group group, PaymentStatus status);

    boolean existsByCoffeeUserAndGroup(CoffeeUser coffeeUser, Group group);
}
