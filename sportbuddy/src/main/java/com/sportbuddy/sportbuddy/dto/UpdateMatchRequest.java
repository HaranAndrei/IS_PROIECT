package com.sportbuddy.sportbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record UpdateMatchRequest(
        @NotBlank String sport,
        @NotNull LocalDateTime startTime,
        @NotNull @Positive Integer maxPlayers,
        @NotNull @PositiveOrZero Integer playersNeeded,
        @NotNull @Positive Long fieldId
) {
}
