package com.pagatu.mail.service;

import com.pagatu.mail.dto.ProssimoPagatoreDto;
import com.pagatu.mail.dto.UltimoPagatoreDto;
import com.pagatu.mail.dto.UtenteDto;
import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.event.ProssimoPagamentoEvent;
import com.pagatu.mail.event.ResetPasswordMailEvent;
import com.pagatu.mail.event.SaltaPagamentoEvent;
import com.pagatu.mail.exception.CustomExceptionEmailSend;
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
 *   <li>Payment notifications for coffee payment rotations</li>
 *   <li>User invitations to groups</li>
 *   <li>Password reset notifications</li>
 *   <li>Payment skip notifications</li>
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
     * <p>
     * This method fetches user data for both the last payer and next payer,
     * then sends a notification email informing the next payer that it's
     * their turn to pay for coffee. The email includes details about the
     * previous payment and the amount due.
     * </p>
     * <p>
     * The operation is performed reactively, fetching user data from the
     * coffee service and sending the email asynchronously to ensure
     * non-blocking execution.
     * </p>
     *
     * @param event the payment event containing payer information and payment details
     * @return a Mono that completes when the email has been sent successfully
     * @throws IllegalArgumentException if event is null
     * @see ProssimoPagamentoEvent
     */
    public Mono<Void> inviaNotificaProssimoPagatore(ProssimoPagamentoEvent event) {
        return fetchUserData(event)
                .flatMap(userData -> sendEmail(event, userData))
                .doOnSuccess(success -> log.info("Email di notifica inviata con successo a {} e {}",
                        event.getProssimoEmail(), event.getUltimoPagatoreEmail()))
                .doOnError(error ->
                        log.error(LOG_ERROR_NOTIFICA, error)
                )
                .then();
    }

    /**
     * Sends a notification email when a payment is skipped.
     * <p>
     * This method notifies the next person in the payment rotation that
     * it's now their turn to pay after someone has skipped their payment.
     * The email is personalized with the recipient's name and includes
     * company branding information.
     * </p>
     * <p>
     * User data is fetched reactively from the coffee service before
     * sending the notification email to ensure accurate recipient information.
     * </p>
     *
     * @param event the skip payment event containing next payer information
     * @return a Mono that completes when the email has been sent successfully
     * @throws IllegalArgumentException if event is null
     * @see SaltaPagamentoEvent
     */
    public Mono<Void> inviaNotificaSaltaPagamento(SaltaPagamentoEvent event) {
        return fetchUserDataSaltaPagamento(event)
                .flatMap(userDataSaltaPagamento -> sendEmailSaltaPagamento(event, userDataSaltaPagamento))
                .doOnSuccess(success -> log.info(LOG_INFO_NOTIFICA + " a {}", event.getProssimoEmail()))
                .doOnError(error ->
                        log.error(LOG_ERROR_NOTIFICA, error)
                )
                .then();
    }

    /**
     * Sends a password reset notification email to the user.
     * <p>
     * This method handles the password reset workflow by fetching user data
     * from the authentication service and sending a secure email containing
     * a reset link with a unique token. The email is personalized with the
     * user's information and provides a secure way to reset their password.
     * </p>
     * <p>
     * The reset link includes a unique token for security and directs users
     * to the frontend application's password reset page.
     * </p>
     *
     * @param event the reset password event containing user email and security token
     * @return a Mono that completes when the email has been sent successfully
     * @throws IllegalArgumentException if event is null
     * @see ResetPasswordMailEvent
     */
    public Mono<Void> inviaNotificaResetPassword(ResetPasswordMailEvent event) {
        return fetchUserDataResetPassword(event)
                .flatMap(userDataResetForgotUserPassword -> sendResetPasswordMail(event, userDataResetForgotUserPassword))
                .doOnSuccess(success -> log.info(LOG_INFO_NOTIFICA + " a {}", event.getEmail()))
                .doOnError(error ->
                        log.error(LOG_ERROR_NOTIFICA, error)
                )
                .then();
    }

    /**
     * Sends an invitation email to a user being invited to join a group.
     * <p>
     * This method sends an email invitation containing a direct link that
     * allows the user to accept the invitation and join the specified group.
     * The invitation link includes encoded parameters for the username and
     * group name to ensure secure and accurate invitation processing.
     * </p>
     * <p>
     * This method operates without fetching additional user data since
     * the invitation event contains all necessary information for generating
     * the invitation email.
     * </p>
     *
     * @param event the invitation event containing user and group information
     * @return a Mono that completes when the email has been sent successfully
     * @throws IllegalArgumentException if event is null
     * @see InvitationEvent
     */
    public Mono<Void> inviaInvitoUtenteNelGruppo(InvitationEvent event) {
        return sendEmailInvitation(event)
                .doOnSuccess(success -> log.info(LOG_INFO_INVITO + " a {}", event.getEmail()))
                .doOnError(error -> log.error(LOG_ERROR_INVITO, error))
                .then();
    }

    /**
     * Fetches user data for both the last payer and next payer from the coffee service.
     * <p>
     * This method makes parallel HTTP requests to retrieve user information
     * for both users involved in the payment notification. The requests are
     * executed concurrently to improve performance and the results are combined
     * into a single UserData record.
     * </p>
     *
     * @param event the payment event containing usernames for both payers
     * @return a Mono containing UserData with both user information
     * @throws IllegalArgumentException if event is null
     */
    private Mono<UserData> fetchUserData(ProssimoPagamentoEvent event) {
        Mono<UltimoPagatoreDto> ultimoPagatoreMono = webClientCoffee.get()
                .uri(API_COFFEE_USER, event.getUltimoPagatoreUsername())
                .retrieve()
                .bodyToMono(UltimoPagatoreDto.class)
                .doOnSuccess(response -> log.info("Ultimo pagatore recuperato con successo: {}", response))
                .doOnError(error -> log.error("Errore durante il recupero dell'ultimo pagatore", error));

        Mono<ProssimoPagatoreDto> prossimoPagatoreMono = webClientCoffee.get()
                .uri(API_COFFEE_USER, event.getProssimoUsername())
                .retrieve()
                .bodyToMono(ProssimoPagatoreDto.class)
                .doOnSuccess(response -> log.info("Prossimo pagatore recuperato con successo: {}", response))
                .doOnError(error -> log.error("Errore durante il recupero del prossimo pagatore", error));

        return Mono.zip(ultimoPagatoreMono, prossimoPagatoreMono)
                .map(tuple -> new UserData(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Fetches user data for the next payer when a payment is skipped.
     * <p>
     * This method retrieves user information from the coffee service
     * for the person who will be the next payer after someone has
     * skipped their payment turn. The data is used to personalize
     * the skip payment notification email.
     * </p>
     *
     * @param event the skip payment event containing the next payer's username
     * @return a Mono containing UserDataSaltaPagamento with the next payer's information
     * @throws IllegalArgumentException if event is null
     */
    private Mono<UserDataSaltaPagamento> fetchUserDataSaltaPagamento(SaltaPagamentoEvent event) {
        return webClientCoffee.get()
                .uri(API_COFFEE_USER, event.getProssimoUsername())
                .retrieve()
                .bodyToFlux(ProssimoPagatoreDto.class)
                .next()
                .doOnSuccess(response -> log.info(LOG_INFO_PAGATORE + ": {}", response))
                .doOnError(error -> log.error(LOG_ERROR_PAGATORE, error))
                .map(UserDataSaltaPagamento::new);
    }

    /**
     * Fetches user data for password reset functionality.
     * <p>
     * This method retrieves user information from the authentication service
     * using the user's email address. This data is used to personalize
     * the password reset email with the user's details and ensure the
     * reset request is legitimate.
     * </p>
     *
     * @param event the reset password event containing the user's email
     * @return a Mono containing UserDataResetForgotUserPassword with user information
     * @throws IllegalArgumentException if event is null
     */
    private Mono<UserDataResetForgotUserPassword> fetchUserDataResetPassword(ResetPasswordMailEvent event) {
        return webClientAuth.get()
                .uri(API_AUTH_USER_GET, event.getEmail())
                .retrieve()
                .bodyToFlux(UtenteDto.class)
                .next()
                .doOnSuccess(response -> log.info(LOG_INFO_UTENTE + ": {}", response))
                .doOnError(error -> log.error(LOG_ERROR_UTENTE, error))
                .map(UserDataResetForgotUserPassword::new);
    }

    /**
     * Sends a password reset email to the user.
     * <p>
     * This method constructs and sends an email containing a secure link
     * that allows the user to reset their forgotten password. The email
     * is personalized with the user's information and includes a unique
     * token for security purposes. The reset link is constructed to match
     * the frontend application's routing structure.
     * </p>
     *
     * @param event                           the reset password event containing token and email information
     * @param userDataResetForgotUserPassword the user data retrieved from the auth service
     * @return a Mono that completes when the email has been sent successfully
     * @throws CustomExceptionEmailSend if there's an error sending the email
     */
    private Mono<Void> sendResetPasswordMail(ResetPasswordMailEvent event, UserDataResetForgotUserPassword userDataResetForgotUserPassword) {
        return Mono.fromRunnable(() -> {
            Context context = getContextForResetPassword(event, userDataResetForgotUserPassword);
            buildAndSendEmail(
                    event.getEmail(),
                    null,
                    "Paga-Tu: Hai richiesto un reset della password",
                    "reset-password",
                    context
            );
        });
    }

    /**
     * Sends a payment notification email to the next payer.
     * <p>
     * This method constructs and sends an email notifying the next person
     * in the rotation that it's their turn to pay for coffee. The email
     * includes information about the previous payer, payment date, amount,
     * and is sent to both the next payer and includes the last payer in BCC
     * for transparency.
     * </p>
     *
     * @param event    the payment event containing payment details
     * @param userData the combined user data for both last and next payers
     * @return a Mono that completes when the email has been sent successfully
     * @throws CustomExceptionEmailSend if there's an error sending the email
     */
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

    /**
     * Sends an invitation email to invite a user to join a group.
     * <p>
     * This method constructs and sends an email invitation containing
     * a direct link that allows the recipient to accept the invitation
     * and join the specified group. The link includes encoded parameters
     * for secure invitation processing and follows the React routing
     * structure of the frontend application.
     * </p>
     *
     * @param event the invitation event containing user and group details
     * @return a Mono that completes when the email has been sent successfully
     * @throws CustomExceptionEmailSend if there's an error sending the email
     */
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

    /**
     * Sends a notification email when a payment has been skipped.
     * <p>
     * This method constructs and sends an email to notify the next person
     * that it's now their turn to pay after someone has skipped their payment.
     * The email is personalized with the recipient's name and company information
     * to maintain a professional and friendly tone.
     * </p>
     *
     * @param event                  the skip payment event containing next payer information
     * @param userDataSaltaPagamento the user data for the next payer
     * @return a Mono that completes when the email has been sent successfully
     * @throws CustomExceptionEmailSend if there's an error sending the email
     */
    private Mono<Void> sendEmailSaltaPagamento(SaltaPagamentoEvent event, UserDataSaltaPagamento userDataSaltaPagamento) {
        return Mono.fromRunnable(() -> {
            Context context = getContextForSaltaPagamento(userDataSaltaPagamento);
            buildAndSendEmail(
                    event.getProssimoEmail(),
                    null,
                    "Paga-Tu: È il tuo turno di pagare il caffè!",
                    "notifica-saltato-pagamento-prossimo-pagatore",
                    context
            );
        });
    }

    /**
     * Builds and sends an email using the specified parameters.
     * <p>
     * This method creates a MIME message with proper headers, processes
     * the Thymeleaf template with the provided context, and sends the email
     * through the configured mail sender. It supports both TO and BCC recipients
     * and includes custom headers for better email delivery and management.
     * </p>
     * <p>
     * The method sets appropriate email headers including:
     * <ul>
     *   <li>X-Mailer for identification</li>
     *   <li>X-Priority for message priority</li>
     *   <li>List-Unsubscribe for compliance</li>
     *   <li>X-Auto-Response-Suppress to prevent auto-replies</li>
     * </ul>
     * </p>
     *
     * @param to           the primary recipient's email address
     * @param bcc          the blind carbon copy recipient's email address (can be null)
     * @param subject      the email subject line
     * @param templateName the name of the Thymeleaf template to process
     * @param context      the Thymeleaf context containing template variables
     * @throws CustomExceptionEmailSend if there's an error creating or sending the email
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
     * <p>
     * This method prepares template variables including payer names,
     * payment date, amount, and company information. All data is
     * formatted appropriately for display in Italian locale with
     * proper date formatting and currency representation.
     * </p>
     *
     * @param event    the payment event containing payment details
     * @param userData the user data for both last and next payers
     * @return a Thymeleaf Context object populated with template variables
     * @throws IllegalArgumentException if event or userData is null
     */
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

    /**
     * Creates a Thymeleaf context for group invitation emails.
     * <p>
     * This method prepares template variables including user information,
     * group details, invitation sender, and generates a secure invitation
     * link with properly encoded parameters. The link follows React routing
     * structure for frontend compatibility and ensures proper URL encoding
     * to handle special characters in usernames and group names.
     * </p>
     *
     * @param event the invitation event containing user and group information
     * @return a Thymeleaf Context object populated with invitation template variables
     * @throws IllegalArgumentException if event is null
     */
    private Context getContextForInvitation(InvitationEvent event) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable("user", event.getUsername());
        context.setVariable("userWhoSentTheInvitation", event.getUserWhoSentTheInvitation());
        context.setVariable("groupName", event.getGroupName());

        // Fixed invitation link to match React routing structure
        String encodedUsername = UriUtils.encode(event.getUsername(), StandardCharsets.UTF_8);
        String encodedGroupName = UriUtils.encode(event.getGroupName(), StandardCharsets.UTF_8);
        String invitationLink = String.format("%s%s?username=%s&groupName=%s",
                domainUrl, invitationUserToGroupPath, encodedUsername, encodedGroupName);

        context.setVariable("link", invitationLink);
        context.setVariable("companyName", company);
        return context;
    }

    /**
     * Creates a Thymeleaf context for skip payment notification emails.
     * <p>
     * This method prepares template variables for notifying the next payer
     * when someone has skipped their payment turn. It includes the next
     * payer's full name and company information to create a personalized
     * and professional notification.
     * </p>
     *
     * @param userDataSaltaPagamento the user data for the next payer
     * @return a Thymeleaf Context object populated with skip payment template variables
     * @throws IllegalArgumentException if userDataSaltaPagamento is null
     */
    private Context getContextForSaltaPagamento(UserDataSaltaPagamento userDataSaltaPagamento) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable("prossimoPagatore",
                userDataSaltaPagamento.prossimoPagatoreDto().getName() + " " + userDataSaltaPagamento.prossimoPagatoreDto().getLastname());
        context.setVariable("companyName", company);
        return context;
    }

    /**
     * Creates a Thymeleaf context for password reset emails.
     * <p>
     * This method prepares template variables for password reset emails,
     * including the user's username and a secure reset link containing
     * the unique token. The reset link is constructed to match the
     * frontend routing structure and includes the security token as a
     * query parameter for verification purposes.
     * </p>
     *
     * @param event                           the reset password event containing the security token
     * @param userDataResetForgotUserPassword the user data from the auth service
     * @return a Thymeleaf Context object populated with reset password template variables
     * @throws IllegalArgumentException if event or userDataResetForgotUserPassword is null
     */
    private Context getContextForResetPassword(ResetPasswordMailEvent event, UserDataResetForgotUserPassword userDataResetForgotUserPassword) {
        Context context = new Context(ITALIAN_LOCALE);
        context.setVariable("user", userDataResetForgotUserPassword.utentedto().getUsername());
        String resetLink = String.format("%s%s?key=%s", domainUrl, resetPswPath, event.getToken());
        context.setVariable("resetLink", resetLink);
        context.setVariable("companyName", company);
        return context;
    }

    /**
     * Record representing user data for payment notifications.
     * <p>
     * This record holds both the last payer and next payer information
     * required for generating payment notification emails. It serves as
     * a data container to combine information from multiple service calls.
     * </p>
     *
     * @param ultimoPagatore   the DTO containing last payer information
     * @param prossimoPagatore the DTO containing next payer information
     */
    private record UserData(UltimoPagatoreDto ultimoPagatore, ProssimoPagatoreDto prossimoPagatore) {
    }

    /**
     * Record representing user data for skip payment notifications.
     * <p>
     * This record holds the next payer information required for
     * generating skip payment notification emails. It encapsulates
     * the user data retrieved from the coffee service.
     * </p>
     *
     * @param prossimoPagatoreDto the DTO containing next payer information
     */
    private record UserDataSaltaPagamento(ProssimoPagatoreDto prossimoPagatoreDto) {
    }

    /**
     * Record representing user data for password reset functionality.
     * <p>
     * This record holds user information retrieved from the authentication
     * service for generating personalized password reset emails. It ensures
     * type safety and immutability for user data handling.
     * </p>
     *
     * @param utentedto the DTO containing user information from auth service
     */
    private record UserDataResetForgotUserPassword(UtenteDto utentedto) {
    }
}