package com.pagatu.mail.listener;

import com.pagatu.mail.event.SaltaPagamentoEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.pagatu.mail.util.Constants.KAFKA_GROUP_ID;

/**
 * Kafka event listener for handling payment skip events.
 * <p>
 * This listener consumes messages from the "saltaPagamento-caffe" Kafka topic
 * and processes events where a scheduled payer has skipped their payment turn.
 * It notifies the next person in the payment rotation via email.
 * </p>
 * This service ensures continuity in the coffee payment rotation system
 * when payment skips occur.
 */
@Log4j2
@Component
public class SaltaPagamentoEventListener {

    private final EmailService emailService;

    public SaltaPagamentoEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "saltaPagamento-caffe", groupId = KAFKA_GROUP_ID, containerFactory = "kafkaListenerContainerFactorySaltaPagamento")
    public void consumeSaltaPagamentoEvent(SaltaPagamentoEvent event) {
        log.info("Ricevuto evento di saltaPagamento: {}", event);
        emailService.inviaNotificaSaltaPagamento(event).subscribe();
    }
}
