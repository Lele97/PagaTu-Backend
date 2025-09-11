package com.pagatu.mail.listener;

import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.pagatu.mail.util.Constants.KAFKA_GROUP_ID;

/**
 * Kafka event listener for handling invitation events.
 * <p>
 * This listener consumes messages from the "invitation-caffe" Kafka topic
 * and processes user invitations to groups by sending email notifications.
 * </p>
 * The listener is part of the mail service microservice and handles
 * asynchronous email delivery for invitation events.
 */
@Log4j2
@Component
public class InvitationEventListener {

    private final EmailService emailService;

    public InvitationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "invitation-caffe", groupId = KAFKA_GROUP_ID, containerFactory = "kafkaListenerContainerFactoryInvitation")
    public void sendEmailInvitation(InvitationEvent event) {
        log.info("Ricevuto evento di invito per utente {} nel gruppo {}",
                event.getUsername(), event.getGroupName());
        emailService.inviaInvitoUtenteNelGruppo(event).subscribe();
    }
}