package com.pagatu.auth.service;

import com.pagatu.auth.entity.EmailVerificationToken;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.EmailVerificationMailEvent;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.exception.InvalidTokenException;
import com.pagatu.auth.exception.RateLimiterException;
import com.pagatu.auth.exception.UserNotFoundException;
import com.pagatu.auth.repository.EmailVerificationTokenRepository;
import com.pagatu.auth.repository.TokenForVerificationRepository;
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
    private final TokenForVerificationRepository tokenForVerificationRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private static final int LIMITER = 10;
    private static final int TOKEN_EXPIRY_MINUTES = 30;


    @Transactional
    public void sendVerificationEmail(User user) {

        String email = user.getEmail();

        log.debug("Processing email verification request for email: {}", email);

        boolean emailExists = userRepository.existsByEmail(email);

        if (!emailExists) {
            log.warn("Password reset requested for non-existent email: {}", email);
            throw new UserNotFoundException("Email not found", email, Constants.EMAIL_EXCEPRION_VALUE);
        }

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentTokenCount = tokenForVerificationRepository.countRecnetTokensByEmail(email, twentyFourHoursAgo);

        if (recentTokenCount >= LIMITER) {
            log.warn("Daily email verification limit exceeded for user: {}", email);
            throw new RateLimiterException("You have exceeded the daily limit for account verification requests.",
                    3600, email);
        }

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setEmail(user.getEmail());
        token.setToken("Paga_Tu_Verify_" + UUID.randomUUID());
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiredDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
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

        EmailVerificationToken verificationToken = tokenForVerificationRepository.findByToken(token);

        if (verificationToken == null) {
            log.warn("Verification token not found: {}", token);
            throw new InvalidTokenException("Token di verifica non valido", "VERIFY_TOKEN");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato", null, "id"));

        if (verificationToken.getTokenStatus().equals(TokenStatus.USED)) {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                log.debug("Token already used but email already verified for user: {}", user.getUsername());
                return;
            }
            log.debug("Token already used: {}", verificationToken.getToken());
            throw new InvalidTokenException("Token di verifica già utilizzato", "VERIFY_TOKEN");
        }

        if (verificationToken.getTokenStatus().equals(TokenStatus.EXPIRED)
                || verificationToken.getExpiredDate().isBefore(LocalDateTime.now())) {
            log.debug("Token has expired for email: {}", verificationToken.getEmail());
            throw new InvalidTokenException("Token di verifica scaduto", "VERIFY_TOKEN");
        }

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