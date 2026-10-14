package com.pagatu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload received by {@code POST /api/auth/oauth/slack}.
 * <p>
 * It carries the authorization {@code code} returned by Slack in the redirect,
 * and optionally the {@code redirect_uri} used to obtain it: Slack rejects the
 * exchange when the value does not match the one registered in the OAuth App.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlackRequest {

    @NotBlank(message = "Il code è obbligatorio")
    private String code;

    /**
     * Optional callback URL. When omitted, the value configured in
     * {@code slack.oauth.redirect-uri} is used.
     */
    private String redirectUri;
}
