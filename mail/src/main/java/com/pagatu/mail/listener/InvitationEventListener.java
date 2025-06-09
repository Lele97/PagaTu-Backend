package com.pagatu.mail.listener;

import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class InvitationEventListener {

    private final EmailService emailService;

    public InvitationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "invitation-caffe", groupId = "email-service", containerFactory = "kafkaListenerContainerFactory_invitation")
    public void sendEmailInvitation(InvitationEvent event){
        log.info("Ricevuto evento di invito per utente {} nel gruppo {}",
                event.getUsername(), event.getGroupName());
        emailService.inviaInvitoUtenteNelGruppo(event).subscribe();
    }

}