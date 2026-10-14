package com.pagatu.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "discord.oauth")
@Data
public class DiscordProperties {
    /**
     * OAuth App client id (public, can be shipped to the frontend).
     */
    private String clientId;

    /**
     * OAuth App client secret (confidential, backend only).
     */
    private String clientSecret;

    /**
     * Callback URL registered in the Discord OAuth App.
     * When set, it is always sent during the code exchange because Discord
     * requires it to match the one used in the authorization request.
     */
    private String redirectUri;

    /**
     * Scope requested during the authorization step.
     * {@code identify} is required for basic user info, {@code email} for email address.
     */
    private String scope = "identify email";

    private String grantType = "authorization_code";

    private String accessTokenUrl = "https://discord.com/api/oauth2/token";

    private String userUrl = "https://discord.com/api/v10/users/@me";

    /**
     * Tells whether Discord login is usable (credentials present).
     *
     * @return true if both client id and client secret are configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
