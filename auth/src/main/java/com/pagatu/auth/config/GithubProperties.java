package com.pagatu.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the GitHub OAuth login flow.
 * <p>
 * The application follows the "authorization code" flow:
 * the frontend redirects the user to GitHub, receives an authorization
 * {@code code} and posts it to {@code /api/auth/oauth/github}; the backend then
 * exchanges that code for an access token using the client credentials defined
 * here.
 * </p>
 * <p>
 * Credentials must never be hardcoded: they are provided through environment
 * variables ({@code GITHUB_CLIENT_ID} / {@code GITHUB_CLIENT_SECRET}).
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "github.oauth")
@Data
public class GithubProperties {

    /**
     * OAuth App client id (public, can be shipped to the frontend).
     */
    private String clientId;

    /**
     * OAuth App client secret (confidential, backend only).
     */
    private String clientSecret;

    /**
     * Callback URL registered in the GitHub OAuth App.
     * When set, it is always sent during the code exchange because GitHub
     * requires it to match the one used in the authorization request.
     */
    private String redirectUri;

    /**
     * Scope requested during the authorization step.
     * {@code user:email} is required to read private email addresses.
     */
    private String scope = "read:user user:email";

    private String accessTokenUrl = "https://github.com/login/oauth/access_token";

    private String userUrl = "https://api.github.com/user";

    private String userEmailsUrl = "https://api.github.com/user/emails";

    /**
     * GitHub API version header, pinned for response stability.
     */
    private String apiVersion = "2022-11-28";

    /**
     * Tells whether GitHub login is usable (credentials present).
     *
     * @return true if both client id and client secret are configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}