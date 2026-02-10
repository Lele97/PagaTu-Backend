package com.pagatu.mail.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.event.NextPaymentEvent;
import com.pagatu.mail.event.ResetPasswordMailEvent;
import com.pagatu.mail.event.SkipPaymentEvent;
import com.pagatu.mail.service.EmailService;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Registers NATS subscriptions and routes messages to appropriate handlers.
 * This replaces the Kafka @KafkaListener annotations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NatsListenerRegistrar {

    private final NatsSubscriber natsSubscriber;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${spring.nats.subject.next-payment:next-payment}")
    private String nextPaymentSubject;

    @Value("${spring.nats.subject.skip-payment:skip-payment}")
    private String skipPaymentSubject;

    @Value("${spring.nats.subject.invitation:invitation}")
    private String invitationSubject;

    @Value("${spring.nats.subject.reset-password-mail:reset-password-mail}")
    private String resetPasswordMailSubject;

    /**
     * Register all NATS subscriptions on startup.
     */
    @PostConstruct
    public void registerSubscriptions() {
        log.info("Registering NATS subscriptions...");

        // Subscribe to next payment events
        natsSubscriber.subscribe(nextPaymentSubject, this::handleNextPaymentEvent);
        log.info("Subscribed to subject: {}", nextPaymentSubject);

        // Subscribe to skip payment events
        natsSubscriber.subscribe(skipPaymentSubject, this::handleSkipPaymentEvent);
        log.info("Subscribed to subject: {}", skipPaymentSubject);

        // Subscribe to invitation events
        natsSubscriber.subscribe(invitationSubject, this::handleInvitationEvent);
        log.info("Subscribed to subject: {}", invitationSubject);

        // Subscribe to reset password mail events
        natsSubscriber.subscribe(resetPasswordMailSubject, this::handleResetPasswordMailEvent);
        log.info("Subscribed to subject: {}", resetPasswordMailSubject);

        log.info("All NATS subscriptions registered successfully");
    }

    /**
     * Handle next payment events.
     */
    private void handleNextPaymentEvent(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received next payment event: {}", json);

            NextPaymentEvent event = objectMapper.readValue(json, NextPaymentEvent.class);
            log.info("Processing payment event for next payer: {}", event.getNextUsername());

            emailService.sendNextPayerNotification(event).subscribe(
                    result -> log.debug("Email sent successfully for payment event"),
                    error -> log.error("Failed to send email for payment event", error));

        } catch (Exception e) {
            log.error("Error processing next payment event", e);
        }
    }

    /**
     * Handle skip payment events.
     */
    private void handleSkipPaymentEvent(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received skip payment event: {}", json);

            SkipPaymentEvent event = objectMapper.readValue(json, SkipPaymentEvent.class);
            log.info("Processing skip payment event for next payer: {}", event.getNextUsername());

            emailService.sendSkipPaymentNotification(event).subscribe(
                    result -> log.debug("Email sent successfully for skip payment event"),
                    error -> log.error("Failed to send email for skip payment event", error));

        } catch (Exception e) {
            log.error("Error processing skip payment event", e);
        }
    }

    /**
     * Handle invitation events.
     */
    private void handleInvitationEvent(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received invitation event: {}", json);

            InvitationEvent event = objectMapper.readValue(json, InvitationEvent.class);
            log.info("Processing invitation event for user {} in group {}",
                    event.getUsername(), event.getGroupName());

            emailService.sendGroupInvitation(event).subscribe(
                    result -> log.debug("Email sent successfully for invitation event"),
                    error -> log.error("Failed to send email for invitation event", error));

        } catch (Exception e) {
            log.error("Error processing invitation event", e);
        }
    }

    /**
     * Handle reset password mail events.
     */
    private void handleResetPasswordMailEvent(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received reset password mail event: {}", json);

            ResetPasswordMailEvent event = objectMapper.readValue(json, ResetPasswordMailEvent.class);
            log.info("Processing reset password event for email: {}", event.getEmail());

            emailService.sendResetPasswordNotification(event).subscribe(
                    result -> log.debug("Email sent successfully for reset password event"),
                    error -> log.error("Failed to send email for reset password event", error));

        } catch (Exception e) {
            log.error("Error processing reset password mail event", e);
        }
    }
}
