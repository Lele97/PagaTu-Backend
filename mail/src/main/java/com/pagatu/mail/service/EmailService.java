package com.pagatu.mail.service;

import com.pagatu.mail.dto.NextPayerDto;
import com.pagatu.mail.dto.LastPayerDto;
import com.pagatu.mail.dto.UserDto;
import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.event.NextPaymentEvent;
import com.pagatu.mail.event.ResetPasswordMailEvent;
import com.pagatu.mail.event.SkipPaymentEvent;
import com.pagatu.mail.exception.CustomExceptionEmailSend;
import com.pagatu.mail.util.Constants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.pagatu.mail.util.Constants.*;

/**
 * Service class responsible for sending various types of email notifications.
 * This service handles email delivery for the Paga-Tu application, including:
 * <ul>
 * <li>Payment notifications for coffee payment rotations</li>
 * <li>User invitations to groups</li>
 * <li>Password reset notifications</li>
 * <li>Payment skip notifications</li>
 * </ul>
 * The service integrates with external microservices to fetch user data and
 * uses Thymeleaf templates for generating HTML email content. All operations
 * are performed reactively using Spring WebFlux to ensure non-blocking
 * execution.
 * Email templates are processed with Italian locale settings and include
 * proper formatting for dates and currency amounts.
 */
@Service
@Log4j2
public class EmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ITALIAN_LOCALE = Locale.ITALIAN;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final WebClient webClientCoffee;
    private final WebClient webClientAuth;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.domainUrl}")
    private String domainUrl;

    @Value("${app.frontend.invitationPath}")
    private String invitationUserToGroupPath;

    @Value("${app.frontend.resetPswPath}")
    private String resetPswPath;

    @Value("${app.frontend.company}")
    private String company;

    public EmailService(JavaMailSender mailSender,
            TemplateEngine templateEngine,
            WebClient.Builder webClientBuilder,
            @Value("${coffee.service.base-url}") String coffeeServiceBaseUrl,
            @Value("${auth.service.base-url}") String authServiceBaseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.webClientCoffee = webClientBuilder.baseUrl(coffeeServiceBaseUrl).build();
        this.webClientAuth = webClientBuilder.baseUrl(authServiceBaseUrl).build();
    }

    /**
     * Sends a payment notification email to the next payer in the rotation.
     *
     * @param event the payment event containing payer information and payment
     *              details
     * @return a Mono that completes when the email has been sent successfully
     */
    public Mono<Void> sendNextPayerNotification(NextPaymentEvent event) {
        return fetchUserData(event)
                .flatMap(userData -> sendPaymentEmail(event, userData))
                .doOnSuccess(success -> log.info("Notification email sent successfully to {} and {}",
                        event.getNextEmail(), event.getLastPayerEmail()))
                .doOnError(error -> log.error(LOG_ERROR_NOTIFICA, error))
                .then();
    }

    /**
     * Sends a notification email when a payment is skipped.
     *
     * @param event the skip payment event containing next payer information
     * @return a Mono that completes when the email has been sent successfully
     */
    public Mono<Void> sendSkipPaymentNotification(SkipPaymentEvent event) {
        return fetchSkipPaymentUserData(event)
                .flatMap(userDataSkipPayment -> sendSkipPaymentEmail(event, userDataSkipPayment))
                .doOnSuccess(success -> log.info("{} to {}", LOG_INFO_NOTIFICA, event.getNextEmail()))
                .doOnError(error -> log.error(LOG_ERROR_NOTIFICA, error))
                .then();
    }

    /**
     * Sends a password reset notification email to the user.
     *
     * @param event the reset password event containing user email and security
     *              token
     * @return a Mono that completes when the email has been sent successfully
     */
    public Mono<Void> sendResetPasswordNotification(ResetPasswordMailEvent event) {
        return fetchResetPasswordUserData(event)
                .flatMap(userDataResetPassword -> sendResetPasswordEmail(event, userDataResetPassword))
                .doOnSuccess(success -> log.info("{} to {}", LOG_INFO_NOTIFICA, event.getEmail()))
                .doOnError(error -> log.error(LOG_ERROR_NOTIFICA, error))
                .then();
    }

    /**
     * Sends an invitation email to a user being invited to join a group.
     *
     * @param event the invitation event containing user and group information
     * @return a Mono that completes when the email has been sent successfully
     */
    public Mono<Void> sendGroupInvitation(InvitationEvent event) {
        return sendInvitationEmail(event)
                .doOnSuccess(success -> log.info("{} to {}", LOG_INFO_INVITO, event.getEmail()))
                .doOnError(error -> log.error(LOG_ERROR_INVITO, error))
                .then();
    }

    /**
     * Fetches user data for both the last payer and next payer from the coffee
     * service.
     */
    private Mono<UserData> fetchUserData(NextPaymentEvent event) {
        Mono<LastPayerDto> lastPayerMono = webClientCoffee.get()
                .uri(API_COFFEE_USER, event.getLastPayerUsername())
                .retrieve()
                .bodyToMono(LastPayerDto.class)
                .doOnSuccess(response -> log.info("Last payer retrieved successfully: {}", response))
                .doOnError(error -> log.error("Error retrieving last payer", error));

        Mono<NextPayerDto> nextPayerMono = webClientCoffee.get()
                .uri(API_COFFEE_USER, event.getNextUsername())
                .retrieve()
                .bodyToMono(NextPayerDto.class)
                .doOnSuccess(response -> log.info("Next payer retrieved successfully: {}", response))
                .doOnError(error -> log.error("Error retrieving next payer", error));

        return Mono.zip(lastPayerMono, nextPayerMono)
                .map(tuple -> new UserData(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Fetches user data for the next payer when a payment is skipped.
     */
    private Mono<UserDataSkipPayment> fetchSkipPaymentUserData(SkipPaymentEvent event) {
        return webClientCoffee.get()
                .uri(API_COFFEE_USER, event.getNextUsername())
                .retrieve()
                .bodyToFlux(NextPayerDto.class)
                .next()
                .doOnSuccess(response -> log.info(LOG_INFO_PAGATORE + ": {}", response))
                .doOnError(error -> log.error(LOG_ERROR_PAGATORE, error))
                .map(UserDataSkipPayment::new);
    }

    /**
     * Fetches user data for password reset functionality.
     */
    private Mono<UserDataResetPassword> fetchResetPasswordUserData(ResetPasswordMailEvent event) {
        return webClientAuth.get()
                .uri(API_AUTH_USER_GET, event.getEmail())
                .retrieve()
                .bodyToFlux(UserDto.class)
                .next()
                .doOnSuccess(response -> log.info(LOG_INFO_UTENTE + ": {}", response))
                .doOnError(error -> log.error(LOG_ERROR_UTENTE, error))
                .map(UserDataResetPassword::new);
    }

    /**
     * Sends a password reset email to the user.
     */
    private Mono<Void> sendResetPasswordEmail(ResetPasswordMailEvent event,
            UserDataResetPassword userDataResetPassword) {
        return Mono.fromRunnable(() -> {
            Context context = getResetPasswordContext(event, userDataResetPassword);
            buildAndSendEmail(
                    event.getEmail(),
                    null,
                    "Paga-Tu: Hai richiesto un reset della password",
                    "reset-password",
                    context);
        });
    }

    /**
     * Sends a payment notification email to the next payer.
     */
    private Mono<Void> sendPaymentEmail(NextPaymentEvent event, UserData userData) {
        return Mono.fromRunnable(() -> {
            Context context = getPaymentContext(event, userData);
            buildAndSendEmail(
                    event.getNextEmail(),
                    event.getLastPayerEmail(),
                    "Paga-Tu: È il tuo turno di pagare il caffè!",
                    "notifica-prossimo-pagatore",
                    context);
        });
    }

    /**
     * Sends an invitation email to invite a user to join a group.
     */
    private Mono<Void> sendInvitationEmail(InvitationEvent event) {
        return Mono.fromRunnable(() -> {
            Context context = getInvitationContext(event);
            buildAndSendEmail(
                    event.getEmail(),
                    null,
                    "Paga-Tu: Sei stato invitato in un gruppo!",
                    "invitation",
                    context);
        });
    }

    /**
     * Sends a notification email when a payment has been skipped.
     */
    private Mono<Void> sendSkipPaymentEmail(SkipPaymentEvent event, UserDataSkipPayment userDataSkipPayment) {
        return Mono.fromRunnable(() -> {
            Context context = getSkipPaymentContext(userDataSkipPayment);
            buildAndSendEmail(
                    event.getNextEmail(),
                    null,
                    "Paga-Tu: È il tuo turno di pagare il caffè!",
                    "notifica-saltato-pagamento-prossimo-pagatore",
                    context);
        });
    }

    /**
     * Builds and sends an email using the specified parameters.
     */
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
            throw new CustomExceptionEmailSend(e.getMessage());
        }
    }

    /**
     * Creates a Thymeleaf context for payment notification emails.
     */
    private Context getPaymentContext(NextPaymentEvent event, UserData userData) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable(Constants.TEMPLATE_VAR_ULTIMO_PAGATORE,
                userData.lastPayer().getName() + " " + userData.lastPayer().getLastname());
        context.setVariable(Constants.TEMPLATE_VAR_PROSSIMO_PAGATORE,
                userData.nextPayer().getName() + " " + userData.nextPayer().getLastname());
        context.setVariable(Constants.TEMPLATE_VAR_DATA_ULTIMO_PAGAMENTO,
                event.getLastPaymentDate().format(DATE_FORMATTER));
        context.setVariable(Constants.TEMPLATE_VAR_IMPORTO, String.format("%.2f€", event.getAmount()));
        context.setVariable(Constants.TEMPLATE_VAR_COMPANY_NAME, company);
        return context;
    }

    /**
     * Creates a Thymeleaf context for group invitation emails.
     */
    private Context getInvitationContext(InvitationEvent event) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable(Constants.TEMPLATE_VAR_USER, event.getUsername());
        context.setVariable(Constants.TEMPLATE_VAR_USER_WHO_SENT_INVITATION, event.getUserWhoSentTheInvitation());
        context.setVariable(Constants.TEMPLATE_VAR_GROUP_NAME, event.getGroupName());

        String encodedUsername = UriUtils.encode(event.getUsername(), StandardCharsets.UTF_8);
        String encodedGroupName = UriUtils.encode(event.getGroupName(), StandardCharsets.UTF_8);
        String invitationLink = String.format("%s%s?username=%s&groupName=%s",
                domainUrl, invitationUserToGroupPath, encodedUsername, encodedGroupName);

        context.setVariable(Constants.TEMPLATE_VAR_LINK, invitationLink);
        context.setVariable(Constants.TEMPLATE_VAR_COMPANY_NAME, company);
        return context;
    }

    /**
     * Creates a Thymeleaf context for skip payment notification emails.
     */
    private Context getSkipPaymentContext(UserDataSkipPayment userDataSkipPayment) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable(Constants.TEMPLATE_VAR_PROSSIMO_PAGATORE,
                userDataSkipPayment.nextPayerDto().getName() + " " + userDataSkipPayment.nextPayerDto().getLastname());
        context.setVariable(Constants.TEMPLATE_VAR_COMPANY_NAME, company);
        return context;
    }

    /**
     * Creates a Thymeleaf context for password reset emails.
     */
    private Context getResetPasswordContext(ResetPasswordMailEvent event, UserDataResetPassword userDataResetPassword) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable(Constants.TEMPLATE_VAR_USER, userDataResetPassword.userDto().getUsername());
        String resetLink = String.format("%s%s?key=%s", domainUrl, resetPswPath, event.getToken());
        context.setVariable(Constants.TEMPLATE_VAR_RESET_LINK, resetLink);
        context.setVariable(Constants.TEMPLATE_VAR_COMPANY_NAME, company);
        return context;
    }

    /**
     * Record representing user data for payment notifications.
     */
    private record UserData(LastPayerDto lastPayer, NextPayerDto nextPayer) {
    }

    /**
     * Record representing user data for skip payment notifications.
     */
    private record UserDataSkipPayment(NextPayerDto nextPayerDto) {
    }

    /**
     * Record representing user data for password reset functionality.
     */
    private record UserDataResetPassword(UserDto userDto) {
    }
}