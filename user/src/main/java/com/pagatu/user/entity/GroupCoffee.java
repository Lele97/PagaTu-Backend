package com.pagatu.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "group_coffee") // Migliore naming per chiarezza
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupCoffee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @ManyToMany(mappedBy = "groupCoffees")
    private Set<User> users;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin;

}
