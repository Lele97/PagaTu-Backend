package com.pagatu.auth.batch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenStatistics {
    private  long totalTokens;
    private  long activeTokens;
    private  long expiredTokens;
    private  long expiredActiveTokens;
}
