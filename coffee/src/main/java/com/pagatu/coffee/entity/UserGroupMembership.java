package com.pagatu.coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity linking a {@link CoffeeUser} to a {@link Group}.
 * <p>
 * Tracks payment rotation state ({@link PaymentStatus}, {@code myTurn}),
 * admin privileges, join date, and the payment history for the membership.
 * </p>
 */
@Entity
@Table(name = "user_group_memberships", uniqueConstraints = @UniqueConstraint(columnNames = { "utente_id",
        "group_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "payments" })
@ToString(exclude = { "coffeeUser", "group", "payments" })
@EqualsAndHashCode(exclude = { "coffeeUser", "group", "payments" })
public class UserGroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    @JsonIgnore
    private CoffeeUser coffeeUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnore
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "my_turn")
    private Boolean myTurn;

    @Column(name = "joined_at")
    private java.time.LocalDateTime joinedAt;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "turn_assigned_at")
    private java.time.LocalDateTime turnAssignedAt;

    @Column(name = "reminder_level")
    private Integer reminderLevel = 0;

    @Column(name = "payment_count")
    private Integer paymentCount = 0;

    @Column(name = "skip_count")
    private Integer skipCount = 0;

    @Column(name = "payment_streak")
    private Integer paymentStreak = 0;

    @Column(name = "round_skip_count")
    private Integer roundSkipCount = 0;

    @OneToMany(mappedBy = "userGroupMembership", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (Boolean.TRUE.equals(isAdmin)) {
            joinedAt = java.time.LocalDateTime.now();
            if (status == null) {
                status = PaymentStatus.NON_PAGATO;
            }
            myTurn = true;
            turnAssignedAt = java.time.LocalDateTime.now();
            reminderLevel = 0;
        } else {
            joinedAt = java.time.LocalDateTime.now();
            if (status == null) {
                status = PaymentStatus.NON_PAGATO;
            }
            myTurn = false;
            reminderLevel = 0;
        }
    }
}