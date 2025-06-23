package com.pagatu.auth.batch;

public class TokenStatistics {
    private final long totalTokens;
    private final long activeTokens;
    private final long expiredTokens;
    private final long expiredActiveTokens;

    public TokenStatistics(long totalTokens, long activeTokens, long expiredTokens, long expiredActiveTokens) {
        this.totalTokens = totalTokens;
        this.activeTokens = activeTokens;
        this.expiredTokens = expiredTokens;
        this.expiredActiveTokens = expiredActiveTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public long getActiveTokens() {
        return activeTokens;
    }

    public long getExpiredTokens() {
        return expiredTokens;
    }

    public long getExpiredActiveTokens() {
        return expiredActiveTokens;
    }

    @Override
    public String toString() {
        return String.format("TokenStatistics{total=%d, active=%d, expired=%d, expiredActive=%d}",
                totalTokens, activeTokens, expiredTokens, expiredActiveTokens);
    }
}
