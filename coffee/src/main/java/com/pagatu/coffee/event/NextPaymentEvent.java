package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published after a payment is registered, indicating who paid and who is next.
 * Consumed by the mail service to notify the next payer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextPaymentEvent {

    private Long lastPaymentId;
    private String lastPayerUsername;
    private String lastPayerEmail;
    private Long nextUserId;
    private String nextUsername;
    private String nextEmail;
    private LocalDateTime lastPaymentDate;
    private Double amount;
    private String groupName;
}
