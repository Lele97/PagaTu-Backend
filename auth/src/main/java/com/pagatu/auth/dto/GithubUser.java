package com.pagatu.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the GitHub user profile ({@code GET https://api.github.com/user})
 * used to identify and provision the user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubUser(
        Long id,
        String login,
        String name,
        String email
) {
}