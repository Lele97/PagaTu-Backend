package com.pagatu.mail.service;

import com.pagatu.mail.event.ProssimoPagamentoEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Log4j2
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void inviaNotificaProssimoPagatore(ProssimoPagamentoEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Imposta mittente e destinatario
            helper.setFrom(fromEmail);
            helper.setTo(event.getProssimoEmail());
            helper.setBcc(event.getUltimoPagatoreEmail());  // invia una copia anche all'ultimo pagatore

            // Imposta oggetto
            helper.setSubject("Paga-Tu: È il tuo turno di pagare il caffè!");

            // Prepare il contesto per il template
            Context context = new Context(Locale.ITALIAN);
            context.setVariable("ultimoPagatore", event.getUltimoPagatoreUsername());
            context.setVariable("prossimoPagatore", event.getProssimoUsername());
            context.setVariable("dataUltimoPagamento",
                    event.getDataUltimoPagamento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            context.setVariable("importo", String.format("%.2f€", event.getImporto()));

            // Genera il contenuto HTML dalla template
            String htmlContent = templateEngine.process("notifica-prossimo-pagatore", context);
            helper.setText(htmlContent, true);

            // Invia l'email
            mailSender.send(message);

            log.info("Email di notifica inviata a {} con successo", event.getProssimoEmail());
        } catch (MessagingException e) {
            log.error("Errore nell'invio dell'email di notifica", e);
            throw new RuntimeException("Errore nell'invio dell'email", e);
        }
    }
}
