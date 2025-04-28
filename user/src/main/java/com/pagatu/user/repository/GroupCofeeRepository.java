package com.pagatu.user.repository;

import com.pagatu.user.entity.GroupCoffee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupCofeeRepository extends JpaRepository<GroupCoffee, Long> {
    Optional<GroupCoffee> findByName(String name);
}
