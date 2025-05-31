package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository  extends JpaRepository<Group, Long> {
    Optional<Group> getGroupByName(String name);
    void deleteGroupByName(String groupName);
}
