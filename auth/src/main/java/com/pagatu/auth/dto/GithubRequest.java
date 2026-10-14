package com.pagatu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload received by {@code POST /api/auth/oauth/github}.
 * <p>
 * It carries the authorization {@code code} returned by GitHub in the redirect,
 * and optionally the {@code redirect_uri} used to obtain it: GitHub rejects the
 * exchange when the value does not match the one registered in the OAuth App.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GithubRequest {

    @NotBlank(message = "Il code è obbligatorio")
    private String code;

    /**
     * Optional callback URL. When omitted, the value configured in
     * {@code github.oauth.redirect-uri} is used.
     */
    private String redirectUri;
}