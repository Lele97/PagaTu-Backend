package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event representing a payment skip notification.
 * Contains details about the next payer when someone skips their turn.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkipPaymentEvent {

    private Long nextUserId;
    private String nextUsername;
    private String nextEmail;
}
