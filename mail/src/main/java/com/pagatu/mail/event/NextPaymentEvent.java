package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event representing a payment notification in the coffee payment system.
 * Contains details about the last payment and the next payer.
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
