package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a user pays the coffee on behalf of another group member.
 * Consumed by the mail service to notify both payer and beneficiary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayForEvent {
    private String payerUsername;
    private String payerEmail;
    private String friendUsername;
    private String friendEmail;
    private String groupName;
    private Double amount;
    private LocalDateTime paymentDate;
}
