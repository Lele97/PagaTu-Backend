package com.pagatu.mail.listener;

import com.pagatu.mail.event.ResetPasswordMailEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.pagatu.mail.util.Constants.KAFKA_GROUP_ID;

/**
 * Kafka event listener for handling password reset email events.
 * <p>
 * This listener consumes messages from the "reset-password-mail" Kafka topic
 * and processes password reset requests by sending email notifications with
 * reset links to users who have requested password changes.
 * </p>
 * The service handles secure password reset workflows by delivering
 * time-sensitive reset tokens via email.
 */
@Component
@Log4j2
public class ResetPasswordMailEventListener {

    private final EmailService emailService;

    public ResetPasswordMailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "reset-password-mail", groupId = KAFKA_GROUP_ID, containerFactory = "kafkaListenerContainerFactoryResetPswMail")
    public void listen(ResetPasswordMailEvent event) {
        log.debug("Ricevuto evento di reset della password {}", event);
        emailService.inviaNotificaResetPassword(event).subscribe();
    }
}
