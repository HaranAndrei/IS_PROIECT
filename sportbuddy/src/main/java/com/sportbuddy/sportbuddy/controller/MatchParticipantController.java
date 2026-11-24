package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.model.MatchParticipant;
import com.sportbuddy.sportbuddy.service.MatchParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchParticipantController {

    private final MatchParticipantService participantService;

    public MatchParticipantController(
            MatchParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping("/{matchId}/participants/me")
    public MatchParticipant joinMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt principal) {

        return participantService.joinMatch(Long.valueOf(principal.getSubject()), matchId);
    }

    @DeleteMapping("/{matchId}/participants/me")
    public ResponseEntity<Void> leaveMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Jwt principal) {
        if (participantService.leaveMatch(Long.valueOf(principal.getSubject()), matchId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{matchId}/participants")
    public List<MatchParticipant> getParticipants(
            @PathVariable Long matchId) {

        return participantService.getParticipants(matchId);
    }
}
