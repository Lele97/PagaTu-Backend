package com.pagatu.auth.repository;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TokenForUserPasswordReset entity operations.
 */
@Repository
public interface TokenForUserPasswordResetRepository extends JpaRepository<TokenForUserPasswordReset, Long> {

    @Query("SELECT count(t) FROM TokenForUserPasswordReset t WHERE t.email = :email AND t.createdAt >= :since")
    long countRecnetTokensByEmail(@Param("email") String email, @Param("since") LocalDateTime since);

    @Query("SELECT t FROM TokenForUserPasswordReset t WHERE t.tokenStatus = :status AND t.expiredDate < :currentTime")
    List<TokenForUserPasswordReset> findExpiredActiveTokens(@Param("status") TokenStatus status, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT t FROM TokenForUserPasswordReset t WHERE t.tokenStatus = :status")
    List<TokenForUserPasswordReset> findAllByTokenStatus(@Param("status") TokenStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE TokenForUserPasswordReset t SET t.tokenStatus = :newStatus WHERE t.expiredDate < :currentTime AND t.tokenStatus = :currentStatus")
    int updateExpiredTokensStatus(@Param("newStatus") TokenStatus newStatus, @Param("currentTime") LocalDateTime currentTime, @Param("currentStatus") TokenStatus currentStatus);

    Optional<TokenForUserPasswordReset> findByToken(String token);

    Optional<TokenForUserPasswordReset> findTokenForUserPasswordResetByToken(String token);
}
