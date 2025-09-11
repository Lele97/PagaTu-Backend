package com.pagatu.mail.util;

/**
 * Utility class containing constants.
 */
public class Constants {

    private Constants() {
    }

    // Kafka Configuration
    public static final String KAFKA_GROUP_ID = "email-service";
    public static final String AUTO_OFFSET_RESET_EARLIEST = "earliest";

    // Trusted Packages
    public static final String TRUSTED_PACKAGE_COFFEE_EVENT = "com.pagatu.coffee.event";
    public static final String TRUSTED_PACKAGE_AUTH_EVENT = "com.pagatu.auth.event";

    // LogMessage
    public static final String LOG_ERROR_NOTIFICA = "Errore nell'invio dell'email di notifica";
    public static final String LOG_ERROR_INVITO = "Errore nell'invio dell'email di invito";
    public static final String LOG_ERROR_PAGATORE = "Errore durante il recupero del prossimo pagatore";
    public static final String LOG_ERROR_UTENTE = "Errore durante il recupero dell'utente";
    public static final String LOG_INFO_NOTIFICA = "Email di notifica inviata con successo";
    public static final String LOG_INFO_INVITO = "Email di invito inviata con successo";
    public static final String LOG_INFO_PAGATORE = "Prossimo pagatore recuperato con successo ";
    public static final String LOG_INFO_UTENTE = "Utente recuperato con successo";

    // API Endpoints
    public static final String API_COFFEE_USER = "/api/coffee/user?username={username}";
    public static final String API_AUTH_USER_GET = "/api/auth/user/get?email={email}";
}
