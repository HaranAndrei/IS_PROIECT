package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.service.MatchService;
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
    public Match createMatch(@RequestBody Match match) {
        return matchService.createMatch(match);
    }

    @GetMapping
    public List<Match> getAllMatches() {
        return matchService.getAllMatches();
    }

    @GetMapping("/sport/{sport}")
    public List<Match> getMatchesBySport(@PathVariable String sport) {
        return matchService.getMatchesBySport(sport);
    }
}