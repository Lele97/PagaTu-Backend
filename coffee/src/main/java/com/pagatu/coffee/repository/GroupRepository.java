package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository  extends JpaRepository<Group, Long> {
    Optional<Group> getGroupByName(String name);
    void deleteGroupByName(String groupName);
    @Query("SELECT g FROM Group g JOIN FETCH g.userMemberships m JOIN FETCH m.utente WHERE g.name = :name")
    Optional<Group> findGroupWithMembershipsByName(@Param("name") String name);

}
