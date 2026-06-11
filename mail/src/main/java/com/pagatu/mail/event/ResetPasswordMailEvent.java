package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NATS payload for a password reset email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordMailEvent {

    private String email;
    private String token;
}
