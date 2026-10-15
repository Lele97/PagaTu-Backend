package com.pagatu.auth.entity;

import com.pagatu.auth.config.ListToStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Entity representing an authenticated application user.
 * <p>
 * Stores credentials, profile data, and optional default group names requested at registration.
 * </p>
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "birthdate", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Convert(converter = ListToStringConverter.class)
    @Column(name = "user_groups", columnDefinition = "TEXT")
    private List<String> groups;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;
}