package com.pagatu.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entry of the GitHub email list ({@code GET https://api.github.com/user/emails}).
 * <p>
 * Required because the profile email can be {@code null} when the user keeps it
 * private; only verified addresses are accepted as proof of ownership.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubUserEmail(
        String email,
        boolean primary,
        boolean verified
) {
}