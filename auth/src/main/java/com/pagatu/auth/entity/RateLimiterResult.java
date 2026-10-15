package com.pagatu.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimiterResult {

    private boolean allowed;
    private long remainingTokens;
    private long waitTimeSeconds;
}
