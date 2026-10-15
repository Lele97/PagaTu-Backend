package com.pagatu.auth.event;

import com.pagatu.auth.entity.EventType;
import com.pagatu.auth.entity.TokenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenForgotPswUserEvent {

    private Long id;
    private LocalDateTime expiredDate;
    private TokenStatus tokenStatus;
    private String token;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
    private EventType eventType;
}
