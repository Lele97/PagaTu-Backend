package com.pagatu.coffee.repository;

import com.pagatu.coffee.dto.CoffeeUserDetailDto;
import com.pagatu.coffee.entity.CoffeeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for CoffeeUser entity operations.
 */
@Repository
public interface CoffeeUserRepository extends JpaRepository<CoffeeUser, Long> {

    @Query("""
                select new com.pagatu.coffee.dto.CoffeeUserDetailDto(
                   u.id,
                        u.authId,
                        u.username,
                        u.email,
                        u.name,
                        u.lastname
                ) from CoffeeUser u where u.username = :username
            """)
    Optional<CoffeeUserDetailDto> findUserByUsername(@Param("username") String username);

    Optional<CoffeeUser> findByUsername(String username);

    CoffeeUser findByEmail(String email);

    Optional<CoffeeUser> findByAuthId(Long authId);
}
