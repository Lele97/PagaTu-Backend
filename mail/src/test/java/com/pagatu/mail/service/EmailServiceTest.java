package com.pagatu.mail.service;

import com.pagatu.mail.event.InvitationResponseEvent;
import com.pagatu.mail.event.PayForEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private WebClient.Builder webClientBuilder;

    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() throws Exception {
        WebClient coffeeClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        WebClient authClient = mock(WebClient.class, RETURNS_DEEP_STUBS);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(coffeeClient, authClient);

        emailService = new EmailService(
                mailSender,
                templateEngine,
                webClientBuilder,
                "http://localhost:8082",
                "http://localhost:8081"
        );

        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@pagatu.app");
        ReflectionTestUtils.setField(emailService, "company", "PagaTu");

        Session session = Session.getDefaultInstance(new Properties());
        mimeMessage = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>test</html>");
    }

    @Test
    void sendPayForNotification_ShouldSendEmailsToPayerAndFriend() {
        // Given
        PayForEvent event = new PayForEvent(
                "payer",
                "payer@example.com",
                "friend",
                "friend@example.com",
                "teamcoffee",
                2.5,
                LocalDateTime.of(2026, 6, 11, 10, 30)
        );

        // When
        emailService.sendPayForNotification(event).block();

        // Then
        verify(mailSender, times(2)).send(any(MimeMessage.class));

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        verify(templateEngine, times(2)).process(templateCaptor.capture(), any(Context.class));

        List<String> templates = templateCaptor.getAllValues();
        assertTrue(templates.contains("pagaper-pagatore"));
        assertTrue(templates.contains("pagaper-ricevente"));
    }

    @Test
    void sendInvitationResponseNotification_WhenAccepted_ShouldUseAcceptedTemplate() {
        // Given
        InvitationResponseEvent event = new InvitationResponseEvent(
                "newuser",
                "admin@example.com",
                "teamcoffee",
                true,
                123L
        );

        // When
        emailService.sendInvitationResponseNotification(event).block();

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine).process(eq("invitation-response-accepted"), any(Context.class));
        verify(templateEngine, never()).process(eq("invitation-response-rejected"), any(Context.class));
    }

    @Test
    void sendInvitationResponseNotification_WhenRejected_ShouldUseRejectedTemplate() {
        // Given
        InvitationResponseEvent event = new InvitationResponseEvent(
                "newuser",
                "admin@example.com",
                "teamcoffee",
                false,
                123L
        );

        // When
        emailService.sendInvitationResponseNotification(event).block();

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine).process(eq("invitation-response-rejected"), any(Context.class));
        verify(templateEngine, never()).process(eq("invitation-response-accepted"), any(Context.class));
    }

    @Test
    void sendPayForNotification_ShouldPopulateTemplateContextWithPaymentData() {
        // Given
        PayForEvent event = new PayForEvent(
                "payer",
                "payer@example.com",
                "friend",
                "friend@example.com",
                "teamcoffee",
                3.0,
                LocalDateTime.of(2026, 6, 11, 15, 0)
        );

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        // When
        emailService.sendPayForNotification(event).block();

        // Then
        verify(templateEngine, times(2)).process(anyString(), contextCaptor.capture());

        Context payerContext = contextCaptor.getAllValues().get(0);
        assertEquals("payer", payerContext.getVariable("payerUsername"));
        assertEquals("friend", payerContext.getVariable("friendUsername"));
        assertEquals("teamcoffee", payerContext.getVariable("groupName"));
        assertEquals("3.00€", payerContext.getVariable("amount"));
        assertEquals("PagaTu", payerContext.getVariable("companyName"));
    }
}