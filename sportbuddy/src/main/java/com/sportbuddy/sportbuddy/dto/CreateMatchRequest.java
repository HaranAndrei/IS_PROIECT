package com.sportbuddy.sportbuddy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

@JsonIgnoreProperties("creator")
public record CreateMatchRequest(
        @NotBlank String sport,
        @NotNull LocalDateTime startTime,
        @Min(1) @Max(2) Integer durationHours,
        @NotNull @Positive Integer maxPlayers,
        @PositiveOrZero Integer playersNeeded,
        @NotNull @Valid Reference field
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reference(@NotNull @Positive Long id) {
    }
}
