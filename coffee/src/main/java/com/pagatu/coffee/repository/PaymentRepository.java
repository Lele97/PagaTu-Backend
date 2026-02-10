package com.pagatu.coffee.repository;

import com.pagatu.coffee.dto.GroupPaymentRankingDto;
import com.pagatu.coffee.entity.Payment;
import com.pagatu.coffee.entity.CoffeeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.userGroupMembership.coffeeUser = :coffeeUser ORDER BY p.paymentDate DESC")
    List<Payment> findByCoffeeUserOrderByPaymentDateDesc(@Param("coffeeUser") CoffeeUser coffeeUser);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.userGroupMembership ugm " +
            "LEFT JOIN FETCH ugm.coffeeUser " +
            "LEFT JOIN FETCH ugm.group " +
            "WHERE p.userGroupMembership.coffeeUser = :coffeeUser " +
            "ORDER BY p.paymentDate DESC")
    List<Payment> findWithUserGroupMembershipByCoffeeUserOrderByPaymentDateDesc(
            @Param("coffeeUser") CoffeeUser coffeeUser);

    @Query("""
                SELECT new com.pagatu.coffee.dto.GroupPaymentRankingDto(u.username,
                    SUM(p.amount),
                    COUNT(p.id)
                )
                FROM Payment p
                JOIN p.userGroupMembership ugm
                JOIN ugm.group g
                JOIN ugm.coffeeUser u
                WHERE g.name = :groupName
                GROUP BY u.username
                ORDER BY SUM(p.amount) DESC
                limit 5
            """)
    List<GroupPaymentRankingDto> getGroupPaymentRanking(@Param("groupName") String groupName);
}
