package com.pagatu.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;

@Repository
@SecondRepository
public interface TokenForUserPasswordResetRepository extends JpaRepository<TokenForUserPasswordReset, Long> {

    Optional<TokenForUserPasswordReset> findTokenForUserPasswordResetByToken(String token);

    @Query("SELECT count(t) FROM TokenForUserPasswordReset t WHERE t.email = :email AND t.createdAt >= :since")
    long countRecnetTokensByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
    
    /**
     * Find all active tokens where expiration date is before the current time
     */
    @Query("SELECT t FROM TokenForUserPasswordReset t WHERE t.tokenStatus = :status AND t.expiredDate < :currentTime")
    List<TokenForUserPasswordReset> findExpiredActiveTokens(@Param("status") TokenStatus status, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all active tokens for batch processing
     */
    @Query("SELECT t FROM TokenForUserPasswordReset t WHERE t.tokenStatus = :status")
    List<TokenForUserPasswordReset> findAllByTokenStatus(@Param("status") TokenStatus status);
    
    /**
     * Batch update token status for expired tokens
     */
    @Modifying
    @Query("UPDATE TokenForUserPasswordReset t SET t.tokenStatus = :newStatus WHERE t.tokenStatus = :currentStatus AND t.expiredDate < :currentTime")
    int updateExpiredTokensStatus(@Param("currentStatus") TokenStatus currentStatus, @Param("newStatus") TokenStatus newStatus, @Param("currentTime") LocalDateTime currentTime);
}
