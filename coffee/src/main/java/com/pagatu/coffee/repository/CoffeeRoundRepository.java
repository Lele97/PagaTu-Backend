package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.CoffeeRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoffeeRoundRepository extends JpaRepository<CoffeeRound, Long> {
    List<CoffeeRound> findByGroupIdOrderByPaymentDateDesc(Long groupId);

    @Query("SELECT cr FROM CoffeeRound cr WHERE cr.groupId = :groupId ORDER BY cr.paymentDate DESC LIMIT 1")
    Optional<CoffeeRound> findLatestByGroupId(@Param("groupId") Long groupId);
}
