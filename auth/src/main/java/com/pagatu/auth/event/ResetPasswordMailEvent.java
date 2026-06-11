package com.pagatu.auth.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbox event published when a user requests a password reset email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordMailEvent {

    private String email;
    private String token;
}
