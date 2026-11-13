package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.repository.MatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public Match createMatch(Match match) {
        return matchRepository.save(match);
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public List<Match> getMatchesBySport(String sport) {
        return matchRepository.findBySportIgnoreCase(sport);
    }
}