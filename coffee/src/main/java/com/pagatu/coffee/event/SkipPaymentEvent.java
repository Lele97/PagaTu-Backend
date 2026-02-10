package com.pagatu.coffee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkipPaymentEvent {

    private Long nextUserId;
    private String nextUsername;
    private String nextEmail;
}
