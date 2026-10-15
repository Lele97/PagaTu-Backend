package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.entity.UserAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAwardRepository extends JpaRepository<UserAward, Long> {

    List<UserAward> findByCoffeeUserOrderByEarnedAtDesc(CoffeeUser coffeeUser);

    boolean existsByCoffeeUserAndCodeAndOccurrenceKey(CoffeeUser coffeeUser, String code, String occurrenceKey);
}
