package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.model.MatchParticipant;
import com.sportbuddy.sportbuddy.service.MatchParticipantService;
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

    @PostMapping("/{matchId}/participants/{userId}")
    public MatchParticipant joinMatch(
            @PathVariable Long matchId,
            @PathVariable Long userId) {

        return participantService.joinMatch(userId, matchId);
    }

    @GetMapping("/{matchId}/participants")
    public List<MatchParticipant> getParticipants(
            @PathVariable Long matchId) {

        return participantService.getParticipants(matchId);
    }
}