package com.pagatu.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the Slack user identity response ({@code GET https://slack.com/api/users.identity})
 * used to identify and provision the user.
 * <p>
 * Slack's {@code users.identity} endpoint returns the user's identity including
 * their user ID, email, name, and team information when the appropriate scopes
 * ({@code identity:read}, {@code users:read}, {@code users:read.email}) are granted.
 * </p>
 *
 * @see <a href="https://api.slack.com/methods/users.identity">Slack users.identity API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackUser(

        @JsonProperty("ok")
        boolean isOK,

        @JsonProperty("user")
        SlackUserInner user,

        @JsonProperty("team")
        SlackTeamInner team,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("error")
        String error,

        @JsonProperty("error_description")
        String errorDescription

) {

    /**
     * Inner class representing the user object in Slack's identity response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SlackUserInner(

            @JsonProperty("id")
            String id,

            @JsonProperty("name")
            String name,

            @JsonProperty("email")
            String email
    ) {}

    /**
     * Inner class representing the team object in Slack's identity response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SlackTeamInner(

            @JsonProperty("id")
            String id,

            @JsonProperty("name")
            String name
    ) {}

    /**
     * Checks if the response indicates a successful identity retrieval.
     *
     * @return true if the response is OK, false otherwise
     */
    public boolean isSuccessful() {
        return isOK && user != null && user.id() != null;
    }

    /**
     * Gets the user's Slack ID.
     *
     * @return the Slack user ID, or null if not available
     */
    public String getUserId() {
        return user != null ? user.id() : null;
    }

    /**
     * Gets the user's email address.
     *
     * @return the user's email, or null if not available
     */
    public String getEmail() {
        return user != null ? user.email() : null;
    }

    /**
     * Gets the user's display name.
     *
     * @return the user's name, or null if not available
     */
    public String getUserName() {
        return user != null ? user.name() : null;
    }

    /**
     * Gets the team ID.
     *
     * @return the Slack team ID, or null if not available
     */
    public String getTeamId() {
        return team != null ? team.id() : null;
    }
}