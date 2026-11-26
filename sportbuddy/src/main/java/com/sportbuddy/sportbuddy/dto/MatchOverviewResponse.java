package com.sportbuddy.sportbuddy.dto;

import java.time.LocalDateTime;

public record MatchOverviewResponse(
        Long id,
        String sport,
        Long fieldId,
        String fieldName,
        Long organizerId,
        LocalDateTime startTime,
        int durationHours,
        int maxPlayers,
        int playersNeeded,
        long availableSpots
) {
}
