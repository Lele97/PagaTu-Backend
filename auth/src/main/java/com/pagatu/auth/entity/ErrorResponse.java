package com.pagatu.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, List<String>> fieldErrors;

    public ErrorResponse(HttpStatus httpStatus, String message, String details, LocalDateTime timestamp) {
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
    }
}
