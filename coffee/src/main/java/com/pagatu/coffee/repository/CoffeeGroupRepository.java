package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.CoffeeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeGroupRepository extends JpaRepository<CoffeeGroup, Long> {
}