package com.pagatu.coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity representing a registered coffee payment.
 * <p>
 * Each payment is associated with a {@link UserGroupMembership} and stores
 * amount, description, and payment timestamp.
 * </p>
 */
@Entity
@Table(name = "pagamento")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_membership", nullable = false)
    @JsonIgnore
    private UserGroupMembership userGroupMembership;

    @Column(name = "importo")
    private Double amount;

    @Column(name = "descrizione")
    private String description;

    @Column(name = "data_pagamento")
    private LocalDateTime paymentDate;
}
