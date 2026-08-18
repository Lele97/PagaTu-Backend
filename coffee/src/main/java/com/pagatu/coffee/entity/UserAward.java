package com.pagatu.coffee.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_awards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_award_occurrence",
                columnNames = {"utente_id", "code", "occurrence_key"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private CoffeeUser coffeeUser;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, length = 64)
    private String icon;

    @Column(name = "occurrence_key", nullable = false, length = 160)
    private String occurrenceKey;

    @Column(name = "group_name", length = 128)
    private String groupName;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    void onCreate() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
    }
}
