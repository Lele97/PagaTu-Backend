package com.pagatu.auth.entity;

/**
 * Supported authentication providers for user accounts.
 * <p>
 * Each provider represents an external identity provider (IdP) that can be used
 * for OAuth2/OIDC authentication. The {@code LOCAL} provider represents the
 * traditional username/password authentication managed by the application itself.
 * </p>
 */
public enum AuthProvider {
    /**
     * Traditional username/password authentication managed locally.
     */
    LOCAL,
    /**
     * Google OAuth2 / OpenID Connect provider.
     */
    GOOGLE,
    /**
     * Discord OAuth2 provider.
     */
    DISCORD,
    /**
     * GitHub OAuth2 provider.
     */
    GITHUB,
    /**
     * Slack OAuth2 provider.
     */
    SLACK
}