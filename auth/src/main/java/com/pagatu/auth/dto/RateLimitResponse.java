package com.pagatu.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitResponse {

    private String message;
    private long retryAfterSeconds;
    private String details;
}
