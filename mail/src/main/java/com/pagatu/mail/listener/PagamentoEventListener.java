package com.pagatu.mail.listener;

import com.pagatu.mail.event.ProssimoPagamentoEvent;
import com.pagatu.mail.service.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.pagatu.mail.util.Constants.KAFKA_GROUP_ID;

/**
 * Kafka event listener for handling payment notification events.
 * <p>
 * This listener consumes messages from the "pagamenti-caffe" Kafka topic
 * and processes payment notifications by sending email alerts to the next
 * person scheduled to pay for coffee.
 * </p>
 * The service is designed to handle coffee payment rotation notifications
 * in a group-based payment system.
 */
@Log4j2
@Component
public class PagamentoEventListener {

    private final EmailService emailService;

    public PagamentoEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "pagamenti-caffe", groupId = KAFKA_GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
    public void consumePagamentoEvent(ProssimoPagamentoEvent event) {
        log.info("Ricevuto evento di pagamento caffè: {}", event);
        emailService.inviaNotificaProssimoPagatore(event).subscribe();
    }
}
