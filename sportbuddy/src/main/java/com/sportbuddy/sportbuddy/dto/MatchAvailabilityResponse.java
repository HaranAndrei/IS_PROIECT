package com.sportbuddy.sportbuddy.dto;

public record MatchAvailabilityResponse(
        int maxPlayers,
        long registeredPlayers,
        long availableSpots
) {
}
