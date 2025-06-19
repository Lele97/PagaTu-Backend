package com.pagatu.auth.repository;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@SecondRepository
public interface TokenForUserPasswordResetRepository extends JpaRepository<TokenForUserPasswordReset, Long> {

    Optional<TokenForUserPasswordReset> findTokenForUserPasswordResetByToken(String token);

    @Query("SELECT count(t) FROM TokenForUserPasswordReset t WHERE t.email = :email AND t.createdAt >= :since")
    long countRecnetTokensByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
}
