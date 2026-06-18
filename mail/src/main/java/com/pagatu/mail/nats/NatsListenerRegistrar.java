package com.pagatu.mail.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.event.InvitationResponseEvent;
import com.pagatu.mail.event.NextPaymentEvent;
import com.pagatu.mail.event.PayForEvent;
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

    @Value("${spring.nats.subject.pay-for:pay-for}")
    private String payForSubject;

    @Value("${spring.nats.subject.invitation-response:invitation-response}")
    private String invitationResponseSubject;

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

        // Subscribe to pay for events
        natsSubscriber.subscribe(payForSubject, this::handlePayForEvent);
        log.info("Subscribed to subject: {}", payForSubject);

        // Subscribe to invitation response events
        natsSubscriber.subscribe(invitationResponseSubject, this::handleInvitationResponseEvent);
        log.info("Subscribed to subject: {}", invitationResponseSubject);

        log.info("All NATS subscriptions registered successfully");
    }

    /**
     * Handles next payment events and triggers the corresponding email.
     *
     * @param msg raw NATS message containing a {@link NextPaymentEvent} JSON payload
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
     * Handles skip payment events and triggers the corresponding email.
     *
     * @param msg raw NATS message containing a {@link SkipPaymentEvent} JSON payload
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
     * Handles group invitation events and triggers the invitation email.
     *
     * @param msg raw NATS message containing an {@link InvitationEvent} JSON payload
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
     * Handles password reset events and triggers the reset email.
     *
     * @param msg raw NATS message containing a {@link ResetPasswordMailEvent} JSON payload
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

    /**
     * Handles pay-on-behalf events and triggers payer/beneficiary emails.
     *
     * @param msg raw NATS message containing a {@link PayForEvent} JSON payload
     */
    private void handlePayForEvent(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received pay for event: {}", json);

            PayForEvent event = objectMapper.readValue(json, PayForEvent.class);
            log.info("Processing pay for event: {} paid for {}", event.getPayerUsername(), event.getFriendUsername());

            emailService.sendPayForNotification(event).subscribe(
                    result -> log.info("Email paga-per inviate: {} -> {} nel gruppo {}",
                            event.getPayerUsername(), event.getFriendUsername(), event.getGroupName()),
                    error -> log.error("Invio email paga-per fallito per {} -> {} nel gruppo {}",
                            event.getPayerUsername(), event.getFriendUsername(), event.getGroupName(), error));

        } catch (Exception e) {
            log.error("Error processing pay for event", e);
        }
    }

    /**
     * Handles invitation response events and notifies the group admin.
     *
     * @param msg raw NATS message containing an {@link InvitationResponseEvent} JSON payload
     */
    private void handleInvitationResponseEvent(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received invitation response event: {}", json);

            InvitationResponseEvent event = objectMapper.readValue(json, InvitationResponseEvent.class);
            log.info("Processing invitation response event: {} responded to invitation in group {} (accepted: {})",
                    event.getUsername(), event.getGroupName(), event.getAccepted());

            emailService.sendInvitationResponseNotification(event).subscribe(
                    result -> log.debug("Email sent successfully for invitation response event"),
                    error -> log.error("Failed to send email for invitation response event", error));

        } catch (Exception e) {
            log.error("Error processing invitation response event", e);
        }
    }
}
