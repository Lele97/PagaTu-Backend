package com.pagatu.coffee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "utenti")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "auth_id", nullable = false)
    private Long authId;

    @Column(name = "status")
    private Status status;

    @Column(name = "name")
    private String name;

    @Column(name = "lastname")
    private String lastname;

    @ManyToMany
    @JoinTable(
            name = "user_groups", // 🌟 tabella di join
            joinColumns = @JoinColumn(name = "utente_id"), // chiave esterna verso Utente
            inverseJoinColumns = @JoinColumn(name = "group_id") // chiave esterna verso Group
    )
    private List<Group> groups;
}
