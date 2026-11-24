package com.sportbuddy.sportbuddy.dto;

import java.time.LocalDateTime;

public record MatchOverviewResponse(
        Long id,
        String sport,
        String fieldName,
        LocalDateTime startTime,
        int maxPlayers,
        long availableSpots
) {
}
