package com.pagatu.auth.repository;

import com.pagatu.auth.entity.EmailVerificationToken;
import com.pagatu.auth.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenAndTokenStatus(String token, TokenStatus status);
}