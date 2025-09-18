package com.pagatu.auth.util;

/**
 * Centralized constants used across the Token Cleanup module.
 */
public final class Constants {

    private Constants() {
    }

    /**
     * API Paths
     */
    public static final String API_BASE_PATH = "/api/admin/token-cleanup";
    public static final String STATISTICS_ENDPOINT = "/statistics";
    public static final String TRIGGER_ENDPOINT = "/trigger";
    public static final String HEALTH_ENDPOINT = "/health";

    /**
     * Response Keys
     */
    public static final String TOTAL_TOKENS = "totalTokens";
    public static final String ACTIVE_TOKENS = "activeTokens";
    public static final String EXPIRED_TOKENS = "expiredTokens";
    public static final String EXPIRED_ACTIVE_TOKENS = "expiredActiveTokens";
    public static final String NEEDS_CLEANUP = "needsCleanup";
    public static final String TIMESTAMP = "timestamp";
    public static final String STATUS = "status";
    public static final String EMAIL_EXCEPRION_VALUE = "email";
    public static final String SUCCESS = "success";
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";
    public static final String BEFORE_STATS = "beforeStats";
    public static final String AFTER_STATS = "afterStats";
    public static final String TOKENS_PROCESSED = "tokensProcessed";

    /**
     * Status Values
     */
    public static final String STATUS_HEALTHY = "healthy";
    public static final String STATUS_UNHEALTHY = "unhealthy";

    /**
     * Log / Messages
     */
    public static final String CLEANUP_SUCCESS_MSG = "Token cleanup completed successfully";
    public static final String CLEANUP_FAILED_MSG = "Token cleanup failed";
    public static final String RETRIEVE_STATS_FAILED_MSG = "Failed to retrieve statistics";
}
