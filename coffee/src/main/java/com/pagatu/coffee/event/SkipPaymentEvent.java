package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a user skips their payment turn.
 * Consumed by the mail service to notify the next payer.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkipPaymentEvent {

    private Long nextUserId;
    private String nextUsername;
    private String nextEmail;
}
