package com.pagatu.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the Discord user profile ({@code GET https://discord.com/api/v10/users/@me})
 * used to identify and provision the user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordUser(String id,
                          String username,

                          @JsonProperty("global_name")
                          String globalName,

                          String email,
                          Boolean verified) {

}
