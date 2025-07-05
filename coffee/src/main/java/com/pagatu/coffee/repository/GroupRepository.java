package com.pagatu.coffee.repository;


import com.pagatu.coffee.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository  extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.userMemberships WHERE g.name = :name")
    Optional<Group> getGroupByName(@Param("name") String name);

    @Query("SELECT DISTINCT g FROM Group g JOIN g.userMemberships m JOIN m.utente u WHERE u.username = :username")
    List<Group> getGroupsByUsername(@Param("username") String username);

    void deleteGroupByName(String groupName);

    @Query("SELECT g FROM Group g JOIN FETCH g.userMemberships m JOIN FETCH m.utente WHERE g.name = :name")
    Optional<Group> findGroupWithMembershipsByName(@Param("name") String name);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.userMemberships")
    List<Group> findAllWithMemberships();

    @Query(value = "SELECT g FROM Group g LEFT JOIN FETCH g.userMemberships",
           countQuery = "SELECT COUNT(g) FROM Group g")
    Page<Group> findAllWithMemberships(Pageable pageable);
}
