package com.pagatu.auth.event;

import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.repository.SecondTokenForUserPassordResetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TokenForgotPswIUserEventConsumer {

    private final SecondTokenForUserPassordResetRepository secondTokenForUserPassordResetRepository;

    public TokenForgotPswIUserEventConsumer(SecondTokenForUserPassordResetRepository secondTokenForUserPassordResetRepository) {
        this.secondTokenForUserPassordResetRepository = secondTokenForUserPassordResetRepository;
    }

    @KafkaListener(topics = "user-service-reset-token", groupId = "auth-service")
    @Transactional("secondTransactionManager")
    public void consume(TokenForgotPswUserEvent tokenForgotPswUserEvent) {
        try {
            switch (tokenForgotPswUserEvent.getEventType()) {
                case CREATE -> handleCreate(tokenForgotPswUserEvent);
                case UPDATE -> handleUpdate(tokenForgotPswUserEvent);
            }
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violazione vincolo UNIQUE per token '{}'. Probabilmente già esistente. Ignorato.",
                    tokenForgotPswUserEvent.getToken());
        } catch (Exception e) {
            log.error("Errore inatteso: {}", e.getMessage(), e);
        }
    }

    public void handleCreate(TokenForgotPswUserEvent tokenForgotPswUserEvent) {
        secondTokenForUserPassordResetRepository.findByToken(tokenForgotPswUserEvent.getToken()).orElseThrow(() -> new RuntimeException("token not found"));
        TokenForUserPasswordReset tokenForUserPasswordReset = new TokenForUserPasswordReset();
        tokenForUserPasswordReset.setToken(tokenForgotPswUserEvent.getToken());
        tokenForUserPasswordReset.setCreatedAt(tokenForgotPswUserEvent.getCreatedAt());
        tokenForUserPasswordReset.setTokenStatus(tokenForgotPswUserEvent.getTokenStatus());
        tokenForUserPasswordReset.setId(tokenForgotPswUserEvent.getId());
        tokenForUserPasswordReset.setExpiredDate(tokenForgotPswUserEvent.getExpiredDate());
        tokenForUserPasswordReset.setEmail(tokenForgotPswUserEvent.getEmail());
        secondTokenForUserPassordResetRepository.save(tokenForUserPasswordReset);
        log.info("Token creato con successo: {}", tokenForUserPasswordReset.getToken());
    }

    public void handleUpdate(TokenForgotPswUserEvent tokenForgotPswUserEvent) {
        secondTokenForUserPassordResetRepository.findByToken(tokenForgotPswUserEvent.getToken()).orElseThrow(() -> new RuntimeException("token not found"));
        TokenForUserPasswordReset tokenForUserPasswordReset = new TokenForUserPasswordReset();
        tokenForUserPasswordReset.setUsedAt(tokenForgotPswUserEvent.getUsedAt());
        tokenForUserPasswordReset.setTokenStatus(tokenForgotPswUserEvent.getTokenStatus());
        secondTokenForUserPassordResetRepository.save(tokenForUserPasswordReset);
        log.info("Token aggiornato con successo: {}", tokenForUserPasswordReset.getToken());
    }
}
