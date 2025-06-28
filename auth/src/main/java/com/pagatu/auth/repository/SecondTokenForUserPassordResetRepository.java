package com.pagatu.auth.repository;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@FirstRepository
public interface SecondTokenForUserPassordResetRepository extends JpaRepository<TokenForUserPasswordReset, Long> {
    Optional<TokenForUserPasswordReset> findByToken(String token);
}
