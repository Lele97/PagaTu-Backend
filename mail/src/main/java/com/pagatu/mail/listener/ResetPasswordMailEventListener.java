package com.pagatu.mail.listener;

import com.pagatu.mail.event.ResetPasswordMailEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ResetPasswordMailEventListener {

    private final EmailService emailService;

    public ResetPasswordMailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "user-service-reset-token", groupId = "email-service", containerFactory = "kafkaListenerContainerFactory_resetPswMail")
    public void listen(ResetPasswordMailEvent event) {
        log.debug("Ricevuto evento di reset della password {}", event);
        emailService.inviaNotificaResetPassword(event).subscribe();
    }
}
