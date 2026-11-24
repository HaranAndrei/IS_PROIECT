package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.dto.CreateMatchRequest;
import com.sportbuddy.sportbuddy.dto.MatchAvailabilityResponse;
import com.sportbuddy.sportbuddy.dto.UpdateMatchRequest;
import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/sport/{sport}")
    public List<Match> getMatchesBySport(@PathVariable String sport) {
        return matchService.getMatchesBySport(sport);
    }

    @GetMapping("/{matchId}/availability")
    public MatchAvailabilityResponse getMatchAvailability(@PathVariable Long matchId) {
        return matchService.getMatchAvailability(matchId);
    }
}
