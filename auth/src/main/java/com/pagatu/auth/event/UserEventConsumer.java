package com.pagatu.auth.event;

import com.pagatu.auth.entity.User;
import com.pagatu.auth.repository.SecondUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserEventConsumer {

    @Autowired
    private SecondUserRepository secondRepository;

    @KafkaListener(topics = "user-service", groupId = "auth-service")
    @Transactional("secondTransactionManager")
    public void consume(UserEvent userEvent) {

        try {
            switch (userEvent.getEventType()) {
                case CREATE -> {
                    try {
                        if (secondRepository.existsByUsername(userEvent.getUsername()) ||
                                secondRepository.existsByEmail(userEvent.getEmail())) {
                            log.warn("Utente con username '{}' o email '{}' già esistente. CREATE ignorato.",
                                    userEvent.getUsername(), userEvent.getEmail());
                            return;
                        }
                        User user = new User();
                        user.setUsername(userEvent.getUsername());
                        user.setEmail(userEvent.getEmail());
                        user.setPassword(userEvent.getPassword());
                        user.setFirstName(userEvent.getFirstName());
                        user.setLastName(userEvent.getLastName());
                        secondRepository.save(user);
                        log.info("Utente creato con successo: {}", user.getUsername());

                    } catch (DataIntegrityViolationException ex) {
                        log.warn("Violazione vincolo UNIQUE per utente '{}'. Probabilmente già esistente. Ignorato.",
                                userEvent.getUsername());
                    } catch (Exception e) {
                        log.error("Errore inatteso durante CREATE: {}", e.getMessage(), e);
                        throw e;
                    }
                }
                case UPDATE, DELETE -> {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
