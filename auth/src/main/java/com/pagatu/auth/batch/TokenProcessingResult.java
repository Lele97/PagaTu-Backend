package com.pagatu.auth.batch;

public class TokenProcessingResult {
    final int processed;
    final int updated;

    TokenProcessingResult(int processed, int updated) {
        this.processed = processed;
        this.updated = updated;
    }
}
