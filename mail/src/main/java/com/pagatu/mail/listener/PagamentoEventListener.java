package com.pagatu.mail.listener;

import com.pagatu.mail.event.ProssimoPagamentoEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class PagamentoEventListener {
    private final EmailService emailService;

    public PagamentoEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "pagamenti-caffe", groupId = "email-service")
    public void consumePagamentoEvent(ProssimoPagamentoEvent event) {
        log.info("Ricevuto evento di pagamento caffè: {}", event);
        emailService.inviaNotificaProssimoPagatore(event).subscribe();
    }
}
