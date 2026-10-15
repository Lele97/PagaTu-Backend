package com.pagatu.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Slack OAuth2 login.
 * <p>
 * Maps the {@code slack.oauth.*} properties from application.yml. The client id
 * is public and can be shipped to the frontend; the client secret must stay
 * backend-only. The redirect URI must match one of the URLs registered in the
 * Slack OAuth App configuration.
 * </p>
 *
 * @see <a href="https://api.slack.com/authentication/oauth-v2">Slack OAuth V2 documentation</a>
 */
@Configuration
@ConfigurationProperties(prefix = "slack.oauth")
@Data
public class SlackProperties {

    /**
     * OAuth App client id (public, can be shipped to the frontend).
     */
    private String clientId;

    /**
     * OAuth App client secret (confidential, backend only).
     */
    private String clientSecret;

    /**
     * Callback URL registered in the Slack OAuth App.
     * When set, it is always sent during the code exchange because Slack
     * requires it to match the one used in the authorization request.
     */
    private String redirectUri;

    /**
     * Scope requested during the authorization step.
     * {@code identity:read} is required for basic identity, {@code users:read}
     * for user info, {@code users:read.email} for email address.
     */
    private String scope = "identity:read users:read users:read.email";

    private String grantType = "authorization_code";

    private String accessTokenUrl = "https://slack.com/api/oauth.v2.access";

    private String userUrl = "https://slack.com/api/users.identity";

    /**
     * Tells whether Slack login is usable (credentials present).
     *
     * @return true if both client id and client secret are configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
