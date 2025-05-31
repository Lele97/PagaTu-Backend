package com.pagatu.coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "userGroupMembership", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pagamento> pagamenti = new ArrayList<>();

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