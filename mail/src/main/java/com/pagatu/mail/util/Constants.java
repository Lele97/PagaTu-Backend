package com.pagatu.mail.util;

/**
 * Utility class containing constants.
 */
public class Constants {

    private Constants() {
    }

    /**
     * Thymeleaf Template Context Keys
     */
    public static final String TEMPLATE_VAR_ULTIMO_PAGATORE = "ultimoPagatore";
    public static final String TEMPLATE_VAR_PROSSIMO_PAGATORE = "prossimoPagatore";
    public static final String TEMPLATE_VAR_DATA_ULTIMO_PAGAMENTO = "dataUltimoPagamento";
    public static final String TEMPLATE_VAR_IMPORTO = "importo";
    public static final String TEMPLATE_VAR_COMPANY_NAME = "companyName";
    public static final String TEMPLATE_VAR_USER = "user";
    public static final String TEMPLATE_VAR_USER_WHO_SENT_INVITATION = "userWhoSentTheInvitation";
    public static final String TEMPLATE_VAR_GROUP_NAME = "groupName";
    public static final String TEMPLATE_VAR_LINK = "link";
    public static final String TEMPLATE_VAR_RESET_LINK = "resetLink";

    /**
     * Kafka Configuration constants
     */
    public static final String KAFKA_GROUP_ID = "email-service";
    public static final String AUTO_OFFSET_RESET_EARLIEST = "earliest";

    /**
     * Trusted Packages constants
     */
    public static final String TRUSTED_PACKAGE_COFFEE_EVENT = "com.pagatu.coffee.event";
    public static final String TRUSTED_PACKAGE_AUTH_EVENT = "com.pagatu.auth.event";

    /**
     * Log message constants
     */
    public static final String LOG_ERROR_NOTIFICA = "Errore nell'invio dell'email di notifica";
    public static final String LOG_ERROR_INVITO = "Errore nell'invio dell'email di invito";
    public static final String LOG_ERROR_PAGATORE = "Errore durante il recupero del prossimo pagatore";
    public static final String LOG_ERROR_UTENTE = "Errore durante il recupero dell'utente";
    public static final String LOG_INFO_NOTIFICA = "Email di notifica inviata con successo";
    public static final String LOG_INFO_INVITO = "Email di invito inviata con successo";
    public static final String LOG_INFO_PAGATORE = "Prossimo pagatore recuperato con successo ";
    public static final String LOG_INFO_UTENTE = "Utente recuperato con successo";

    /**
     * API Endpoints constants
     */
    public static final String API_COFFEE_USER = "/api/coffee/user?username={username}";
    public static final String API_AUTH_USER_GET = "/api/auth/user/get?email={email}";
}
