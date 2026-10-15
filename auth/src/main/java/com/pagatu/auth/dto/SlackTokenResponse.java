package com.pagatu.auth.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned by the Slack token endpoint
 * ({@code https://slack.com/api/oauth.v2.access}).
 * <p>
 * Discord answers with HTTP 200 on success, or 400 with error details on failure.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackTokenResponse(

        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("ok") boolean isOK,
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription


) {
}
