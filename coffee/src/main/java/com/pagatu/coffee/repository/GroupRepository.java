package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Group entity operations.
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.userMemberships WHERE g.name = :name")
    Optional<Group> getGroupByName(@Param("name") String name);

    @Query("SELECT DISTINCT g FROM Group g JOIN g.userMemberships m JOIN m.coffeeUser u WHERE u.username = :username")
    List<Group> getGroupsByUsername(@Param("username") String username);

    @Query("SELECT g FROM Group g JOIN FETCH g.userMemberships m JOIN FETCH m.coffeeUser WHERE g.name = :name")
    Optional<Group> findGroupWithMembershipsByName(@Param("name") String name);

    void deleteGroupByName(String groupName);
}
