package com.pagatu.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned by the Discord token endpoint
 * ({@code https://discord.com/api/oauth2/token}).
 * <p>
 * Discord answers with HTTP 200 on success, or 400 with error details on failure.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
) {
}