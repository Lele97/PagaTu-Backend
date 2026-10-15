package com.pagatu.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned by the GitHub token endpoint
 * ({@code https://github.com/login/oauth/access_token}).
 * <p>
 * GitHub answers with HTTP 200 even when the exchange fails, reporting the
 * problem in the {@code error} / {@code error_description} fields, so both
 * success and failure payloads are modelled here.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription
) {
}