package com.sportbuddy.sportbuddy.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class Match {

    public static final int DEFAULT_DURATION_HOURS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private Integer durationHours;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false)
    private int playersNeeded;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne(optional = false)
    @JoinColumn(name = "field_id")
    private SportsField field;

    public Match() {
    }

    public Match(String sport, LocalDateTime startTime, int maxPlayers,
                 int playersNeeded, User creator, SportsField field) {
        this.sport = sport;
        this.startTime = startTime;
        this.maxPlayers = maxPlayers;
        this.playersNeeded = playersNeeded;
        this.creator = creator;
        this.field = field;
    }

    public Long getId() {
        return id;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationHours() {
        return durationHours == null ? DEFAULT_DURATION_HOURS : durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPlayersNeeded() {
        return playersNeeded;
    }

    public void setPlayersNeeded(int playersNeeded) {
        this.playersNeeded = playersNeeded;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public SportsField getField() {
        return field;
    }

    public void setField(SportsField field) {
        this.field = field;
    }
}
