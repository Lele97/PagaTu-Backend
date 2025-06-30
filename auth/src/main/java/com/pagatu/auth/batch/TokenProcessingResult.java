package com.pagatu.auth.batch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenProcessingResult {
    int processed;
    int updated;
}
