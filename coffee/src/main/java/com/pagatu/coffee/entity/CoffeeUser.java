package com.pagatu.coffee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Entity representing a user in the coffee domain.
 * <p>
 * Users are synchronized from the auth service and linked to groups through
 * {@link UserGroupMembership}. The {@code authId} stores the identifier from the JWT.
 * </p>
 */
@Entity
@Table(name = "utenti")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "groupMemberships")
@EqualsAndHashCode(exclude = "groupMemberships")
public class CoffeeUser {

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

    @Column(name = "satispay_link")
    private String satispayLink;

    @Column(name = "revolut_link")
    private String revolutLink;

    @OneToMany(mappedBy = "coffeeUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserGroupMembership> groupMemberships;
}
