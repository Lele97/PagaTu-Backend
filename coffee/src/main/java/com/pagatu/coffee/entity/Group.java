package com.pagatu.coffee.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a coffee payment group.
 * <p>
 * A group aggregates members who rotate coffee payments. Each group has a unique name,
 * an optional description, and a collection of {@link UserGroupMembership} records.
 * </p>
 */
@Entity
@Data
@Table(name = "user_group")
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    /**
     * Jackson constructor used when deserializing a group by name only.
     *
     * @param name unique group name
     */
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

    @Column(name = "max_skip_per_round")
    private Integer maxSkipPerRound;

    @Column(name = "pay_for_enabled")
    private Boolean payForEnabled = true;

    @Column(name = "pay_for_admin_only")
    private Boolean payForAdminOnly = false;

    @Column(name = "current_round_number")
    private Integer currentRoundNumber = 1;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserGroupMembership> userMemberships = new ArrayList<>();
}