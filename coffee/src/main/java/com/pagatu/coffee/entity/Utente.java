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

    @Column(name = "name")
    private String name;

    @Column(name = "lastname")
    private String lastname;

    // Replace direct many-to-many with membership relationship
    @OneToMany(mappedBy = "utente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGroupMembership> groupMemberships;

    // Helper method to get groups (for backward compatibility)
    public List<Group> getGroups() {
        if (groupMemberships == null) return List.of();
        return groupMemberships.stream()
                .map(UserGroupMembership::getGroup)
                .toList();
    }

    // Helper method to get status for a specific group
    public Status getStatusForGroup(Group group) {
        if (groupMemberships == null) return null;
        return groupMemberships.stream()
                .filter(membership -> membership.getGroup().equals(group))
                .findFirst()
                .map(UserGroupMembership::getStatus)
                .orElse(null);
    }

    // Helper method to check if user is admin of a specific group
    public boolean isAdminOfGroup(Group group) {
        if (groupMemberships == null) return false;
        return groupMemberships.stream()
                .filter(membership -> membership.getGroup().equals(group))
                .findFirst()
                .map(UserGroupMembership::getIsAdmin)
                .orElse(false);
    }
}