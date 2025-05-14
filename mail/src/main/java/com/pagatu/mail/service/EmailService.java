package com.pagatu.mail.service;

import com.pagatu.mail.dto.ProssimoPagatoreDto;
import com.pagatu.mail.dto.UltimoPagatoreDto;
import com.pagatu.mail.event.ProssimoPagamentoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Log4j2
public class EmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ITALIAN_LOCALE = Locale.ITALIAN;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final WebClient webClient;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        WebClient.Builder webClientBuilder,
                        @Value("${coffee.service.base-url}") String coffeeServiceBaseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.webClient = webClientBuilder.baseUrl(coffeeServiceBaseUrl).build();
    }

    public Mono<Void> inviaNotificaProssimoPagatore(ProssimoPagamentoEvent event) {
        return fetchUserData(event)
                .flatMap(userData -> sendEmail(event, userData))
                .doOnSuccess(__ -> log.info("Email di notifica inviata con successo a {} e {}",
                        event.getProssimoEmail(), event.getUltimoPagatoreEmail()))
                .doOnError(error ->
                        log.error("Errore nell'invio dell'email di notifica", error)
                )
                .then();
    }
    private Mono<UserData> fetchUserData(ProssimoPagamentoEvent event) {
        Mono<UltimoPagatoreDto> ultimoPagatoreMono = webClient.get()
                .uri("/api/coffee/user?username={username}", event.getUltimoPagatoreUsername())
                .retrieve()
                .bodyToMono(UltimoPagatoreDto.class)
                .doOnSuccess(response -> log.info("Ultimo pagatore recuperato con successo: {}", response))
                .doOnError(error -> log.error("Errore durante il recupero dell'ultimo pagatore", error));

        Mono<ProssimoPagatoreDto> prossimoPagatoreMono = webClient.get()
                .uri("/api/coffee/user?username={username}", event.getProssimoUsername())
                .retrieve()
                .bodyToMono(ProssimoPagatoreDto.class)
                .doOnSuccess(response -> log.info("Prossimo pagatore recuperato con successo: {}", response))
                .doOnError(error -> log.error("Errore durante il recupero del prossimo pagatore", error));

        return Mono.zip(ultimoPagatoreMono, prossimoPagatoreMono)
                .map(tuple -> new UserData(tuple.getT1(), tuple.getT2()));
    }

    private Mono<Void> sendEmail(ProssimoPagamentoEvent event, UserData userData) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(event.getProssimoEmail());
                helper.setBcc(event.getUltimoPagatoreEmail());
                helper.setSubject("Paga-Tu: È il tuo turno di pagare il caffè!");

                Context context = new Context(ITALIAN_LOCALE);
                context.setVariable("ultimoPagatore",
                        userData.ultimoPagatore().getName() + " " + userData.ultimoPagatore().getLastname());
                context.setVariable("prossimoPagatore",
                        userData.prossimoPagatore().getName() + " " + userData.prossimoPagatore().getLastname());
                context.setVariable("dataUltimoPagamento",
                        event.getDataUltimoPagamento().format(DATE_FORMATTER));
                context.setVariable("importo", String.format("%.2f€", event.getImporto()));

                String htmlContent = templateEngine.process("notifica-prossimo-pagatore", context);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                return null;
            } catch (MessagingException e) {
                throw new RuntimeException("Errore nell'invio dell'email", e);
            }
        });
    }

    private record UserData(UltimoPagatoreDto ultimoPagatore, ProssimoPagatoreDto prossimoPagatore) {
    }
}