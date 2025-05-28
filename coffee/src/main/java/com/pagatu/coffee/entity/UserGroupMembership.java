package com.pagatu.coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_group_memberships")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // Additional fields for membership metadata
    @Column(name = "joined_at")
    private java.time.LocalDateTime joinedAt;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @PrePersist
    protected void onCreate() {
        joinedAt = java.time.LocalDateTime.now();
        if (status == null) {
            status = Status.NON_PAGATO; // Default status
        }
    }

    // Constructor for easy creation
    public UserGroupMembership(Utente utente, Group group, Status status) {
        this.utente = utente;
        this.group = group;
        this.status = status;
        this.isAdmin = false;
    }
}