package com.pagatu.mail.listener;

import com.pagatu.mail.event.SaltaPagamentoEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class SaltaPagamentoEventListener {

    private final EmailService emailService;

    public SaltaPagamentoEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "saltaPagamento-caffe", groupId = " ", containerFactory = "kafkaListenerContainerFactory_saltaPagamento")
    public void consumeSaltaPagamentoEvent(SaltaPagamentoEvent event) {
        log.info("Ricevuto evento di saltaPagamento: {}", event);
        emailService.inviaNotificaSaltaPagamento(event).subscribe();
    }
}
