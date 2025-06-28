package com.pagatu.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "token_password_reset")
public class TokenForUserPasswordReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expired_date")
    private LocalDateTime expiredDate;

    @Enumerated(EnumType.STRING)
    private TokenStatus tokenStatus;

    private String token;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
