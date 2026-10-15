package com.pagatu.mail.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NATS payload for pay-on-behalf notification emails.
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
