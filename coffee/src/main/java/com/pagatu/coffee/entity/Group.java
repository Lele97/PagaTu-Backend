package com.pagatu.coffee.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@Table(name = "groups")
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    @JsonCreator
    public Group(@JsonProperty("name") String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    // Replace direct many-to-many with membership relationship
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGroupMembership> userMemberships;

    // Helper method to get users (for backward compatibility)
    public List<Utente> getUsers() {
        if (userMemberships == null) return List.of();
        return userMemberships.stream()
                .map(UserGroupMembership::getUtente)
                .toList();
    }

    // Helper method to get users with specific status
    public List<Utente> getUsersWithStatus(Status status) {
        if (userMemberships == null) return List.of();
        return userMemberships.stream()
                .filter(membership -> membership.getStatus() == status)
                .map(UserGroupMembership::getUtente)
                .toList();
    }

    // Helper method to get group admins
    public List<Utente> getAdmins() {
        if (userMemberships == null) return List.of();
        return userMemberships.stream()
                .filter(membership -> Boolean.TRUE.equals(membership.getIsAdmin()))
                .map(UserGroupMembership::getUtente)
                .toList();
    }
}