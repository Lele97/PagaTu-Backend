package com.pagatu.mail.service;

import com.pagatu.mail.dto.ProssimoPagatoreDto;
import com.pagatu.mail.dto.UltimoPagatoreDto;
import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.event.ProssimoPagamentoEvent;
import com.pagatu.mail.event.SaltaPagamentoEvent;
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

    @Value("${app.frontend.base-url}")
    private String baseUrl;

    @Value("${app.frontend.path}")
    private String requestPath;

    @Value("${app.frontend.company}")
    private String company;

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

    public Mono<Void> inviaNotificaSaltaPagamento(SaltaPagamentoEvent event) {
        return fetchUserData_SaltaPagamento(event)
                .flatMap(userDataSaltaPagamento -> sendEmailSaltaPagamento(event, userDataSaltaPagamento))
                .doOnSuccess(__ -> log.info("Email di notifica inviata con successo a {}",
                        event.getProssimoEmail()))
                .doOnError(error ->
                        log.error("Errore nell'invio dell'email di notifica", error)
                )
                .then();
    }

    public Mono<Void> inviaInvitoUtenteNelGruppo(InvitationEvent event) {
        return sendEmailInvitation(event)
                .doOnSuccess(__ -> log.info("Email di invito inviata con successo a {}", event.getEmail()))
                .doOnError(error -> log.error("Errore nell'invio dell'email di invito", error))
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

    private Mono<UserData_SaltaPagamento> fetchUserData_SaltaPagamento(SaltaPagamentoEvent event) {
        return webClient.get()
                .uri("/api/coffee/user?username={username}", event.getProssimoUsername())
                .retrieve()
                .bodyToFlux(ProssimoPagatoreDto.class)
                .next()
                .doOnSuccess(response -> log.info("Prossimo pagatore recuperato con successo: {}", response))
                .doOnError(error -> log.error("Errore durante il recupero del prossimo pagatore", error))
                .map(UserData_SaltaPagamento::new);
    }

    private Mono<Void> sendEmail(ProssimoPagamentoEvent event, UserData userData) {
        return Mono.fromRunnable(() -> {
            Context context = getContext(event, userData);
            buildAndSendEmail(
                    event.getProssimoEmail(),
                    event.getUltimoPagatoreEmail(),
                    "Paga-Tu: È il tuo turno di pagare il caffè!",
                    "notifica-prossimo-pagatore",
                    context
            );
        });
    }

    private Mono<Void> sendEmailInvitation(InvitationEvent event) {
        return Mono.fromRunnable(() -> {
            Context context = getContextForInvitation(event);
            buildAndSendEmail(
                    event.getEmail(),
                    null,
                    "Paga-Tu: Sei stato invitato in un gruppo!",
                    "invitation",
                    context
            );
        });
    }

    private Mono<Void> sendEmailSaltaPagamento(SaltaPagamentoEvent event, UserData_SaltaPagamento userDataSaltaPagamento) {
        return Mono.fromRunnable(() -> {
            Context context = getContextForSaltaPagamento(event, userDataSaltaPagamento);
            buildAndSendEmail(
                    event.getProssimoEmail(),
                    null,
                    "Paga-Tu: È il tuo turno di pagare il caffè!",
                    "notifica-saltato-pagamento-prossimo-pagatore",
                    context
            );
        });
    }

    private void buildAndSendEmail(String to, String bcc, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            message.setHeader("X-Mailer", company + " Invitation System");
            message.setHeader("X-Priority", "3");
            message.setHeader("List-Unsubscribe", "<mailto:" + fromEmail + "?subject=unsubscribe>");
            message.setHeader("X-Auto-Response-Suppress", "OOF, DR, RN, NRN");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            if (bcc != null) {
                helper.setBcc(bcc);
            }
            helper.setSubject(subject);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Errore nell'invio dell'email", e);
        }
    }

    private Context getContext(ProssimoPagamentoEvent event, UserData userData) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable("ultimoPagatore",
                userData.ultimoPagatore().getName() + " " + userData.ultimoPagatore().getLastname());
        context.setVariable("prossimoPagatore",
                userData.prossimoPagatore().getName() + " " + userData.prossimoPagatore().getLastname());
        context.setVariable("dataUltimoPagamento",
                event.getDataUltimoPagamento().format(DATE_FORMATTER));
        context.setVariable("importo", String.format("%.2f€", event.getImporto()));
        context.setVariable("companyName", company);
        return context;
    }

    private Context getContextForInvitation(InvitationEvent event) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable("user", event.getUsername());
        context.setVariable("userWhoSentTheInvitation", event.getUserWhoSentTheInvitation());
        context.setVariable("groupName", event.getGroupName());

        String invitationLink = String.format("%s%s?username=%s&groupName=%s",
                baseUrl, requestPath, event.getUsername(), event.getGroupName());
        context.setVariable("link", invitationLink);
        context.setVariable("companyName", company);
        return context;
    }

    private Context getContextForSaltaPagamento(SaltaPagamentoEvent event, UserData_SaltaPagamento userDataSaltaPagamento) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable("prossimoPagatore",
                userDataSaltaPagamento.prossimoPagatoreDto().getName() + " " + userDataSaltaPagamento.prossimoPagatoreDto().getLastname());
        context.setVariable("companyName", company);
        return context;
    }

    private record UserData(UltimoPagatoreDto ultimoPagatore, ProssimoPagatoreDto prossimoPagatore) {
    }

    private record UserData_SaltaPagamento(ProssimoPagatoreDto prossimoPagatoreDto) {
    }
}
