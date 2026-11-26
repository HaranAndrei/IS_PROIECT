package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.dto.CreateMatchRequest;
import com.sportbuddy.sportbuddy.dto.MatchAvailabilityResponse;
import com.sportbuddy.sportbuddy.dto.MatchOverviewResponse;
import com.sportbuddy.sportbuddy.dto.UpdateMatchRequest;
import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.service.MatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public Match createMatch(@AuthenticationPrincipal Jwt principal, @Valid @RequestBody CreateMatchRequest request) {
        return matchService.createMatch(Long.valueOf(principal.getSubject()), request);
    }

    @PutMapping("/{matchId}")
    public Match updateMatch(@PathVariable Long matchId, @AuthenticationPrincipal Jwt principal,
                             @Valid @RequestBody UpdateMatchRequest request) {
        return matchService.updateMatch(matchId, Long.valueOf(principal.getSubject()), request);
    }

    @DeleteMapping("/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(@PathVariable Long matchId, @AuthenticationPrincipal Jwt principal) {
        matchService.deleteMatch(matchId, Long.valueOf(principal.getSubject()));
    }

    @GetMapping
    public List<Match> getAllMatches() {
        return matchService.getAllMatches();
    }

    @GetMapping("/overview")
    public List<MatchOverviewResponse> getMatchOverview() {
        return matchService.getMatchOverview();
    }

    @GetMapping("/available")
    public List<MatchOverviewResponse> getAvailableMatches(@AuthenticationPrincipal Jwt principal) {
        return matchService.getAvailableMatches(Long.valueOf(principal.getSubject()));
    }

    @GetMapping("/available-slots")
    public List<LocalTime> getAvailableSlots(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam Long fieldId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @Min(1) @Max(2) int durationHours,
            @RequestParam(required = false) Long matchId) {
        return matchService.getAvailableSlots(Long.valueOf(principal.getSubject()), fieldId, date, durationHours, matchId);
    }

    @GetMapping("/sport/{sport}")
    public List<Match> getMatchesBySport(@PathVariable String sport) {
        return matchService.getMatchesBySport(sport);
    }

    @GetMapping("/{matchId}/availability")
    public MatchAvailabilityResponse getMatchAvailability(@PathVariable Long matchId) {
        return matchService.getMatchAvailability(matchId);
    }
}
