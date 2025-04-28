package com.pagatu.coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coffee_rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long payerId;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = true)
    private Double amount;

    private String notes;

    // Next person who should pay
    @Column(nullable = true)
    private Long nextPayerId;

    // When notification was sent
    private LocalDateTime notificationSent;
}