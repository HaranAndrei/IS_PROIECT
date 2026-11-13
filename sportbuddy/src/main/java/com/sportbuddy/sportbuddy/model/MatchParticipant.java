package com.sportbuddy.sportbuddy.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_participants",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "match_id"}
        )
)
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public MatchParticipant() {
    }

    public MatchParticipant(User user, Match match) {
        this.user = user;
        this.match = match;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Match getMatch() {
        return match;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}