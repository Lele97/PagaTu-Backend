package com.pagatu.auth.service;

import com.pagatu.auth.entity.EmailVerificationToken;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.EmailVerificationMailEvent;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.exception.InvalidTokenException;
import com.pagatu.auth.exception.UserNotFoundException;
import com.pagatu.auth.repository.EmailVerificationTokenRepository;
import com.pagatu.auth.repository.UserRepository;
import com.pagatu.auth.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int TOKEN_EXPIRY_HOURS = 48;

    @Value("${spring.nats.subject.verify-email-mail:verify-email-mail}")
    private String verifyEmailSubject;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;

    @Transactional
    public void sendVerificationEmail(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken("Paga_Tu_Verify_" + UUID.randomUUID());
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiredDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        token.setTokenStatus(TokenStatus.ACTIVE);

        EmailVerificationToken saved = tokenRepository.save(token);

        EmailVerificationMailEvent event = new EmailVerificationMailEvent();
        event.setEmail(user.getEmail());
        event.setUsername(user.getUsername());
        event.setToken(saved.getToken());

        outboxService.saveEvent(verifyEmailSubject, event);
        log.info("Email di verifica inviata a {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {

        log.info(token);

        EmailVerificationToken verificationToken = tokenRepository
                .findByTokenAndTokenStatus(token, TokenStatus.ACTIVE)
                .orElseThrow(() -> new InvalidTokenException("Token di verifica non valido", "VERIFY_TOKEN"));

        if (verificationToken.getExpiredDate().isBefore(LocalDateTime.now())) {
            verificationToken.setTokenStatus(TokenStatus.EXPIRED);
            tokenRepository.save(verificationToken);
            throw new InvalidTokenException("Token di verifica scaduto", "VERIFY_TOKEN");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato", null, "id"));

        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setTokenStatus(TokenStatus.USED);
        tokenRepository.save(verificationToken);
        log.info("Email verificata per utente {}", user.getUsername());
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.getByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato", email, Constants.EMAIL_EXCEPRION_VALUE));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AuthenticationException("L'email è già verificata");
        }

        sendVerificationEmail(user);
    }
}