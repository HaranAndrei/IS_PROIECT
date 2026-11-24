package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.dto.CreateMatchRequest;
import com.sportbuddy.sportbuddy.dto.MatchAvailabilityResponse;
import com.sportbuddy.sportbuddy.dto.MatchOverviewResponse;
import com.sportbuddy.sportbuddy.dto.UpdateMatchRequest;
import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.MatchParticipantRepository;
import com.sportbuddy.sportbuddy.repository.MatchRepository;
import com.sportbuddy.sportbuddy.repository.SportsFieldRepository;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final SportsFieldRepository sportsFieldRepository;
    private final MatchParticipantRepository participantRepository;

    public MatchService(MatchRepository matchRepository,
                        UserRepository userRepository,
                        SportsFieldRepository sportsFieldRepository,
                        MatchParticipantRepository participantRepository) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.sportsFieldRepository = sportsFieldRepository;
        this.participantRepository = participantRepository;
    }

    public Match createMatch(Long organizerId, CreateMatchRequest request) {
        User creator = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organizatorul nu exista"));
        SportsField field = findField(request.field().id());

        int playersNeeded = Objects.requireNonNullElse(request.playersNeeded(), 0);
        Match match = new Match(request.sport(), request.startTime(), request.maxPlayers(),
                playersNeeded, creator, field);
        return matchRepository.save(match);
    }

    @Transactional
    public Match updateMatch(Long matchId, Long organizerId, UpdateMatchRequest request) {
        Match match = findOwnedMatchForUpdate(matchId, organizerId);
        SportsField field = findField(request.fieldId());

        if (participantRepository.countByMatchId(matchId) > request.maxPlayers()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Capacitatea este mai mica decat numarul de jucatori inscrisi");
        }

        match.setSport(request.sport());
        match.setStartTime(request.startTime());
        match.setMaxPlayers(request.maxPlayers());
        match.setPlayersNeeded(request.playersNeeded());
        match.setField(field);
        return match;
    }

    @Transactional
    public void deleteMatch(Long matchId, Long organizerId) {
        Match match = findOwnedMatchForUpdate(matchId, organizerId);
        participantRepository.deleteByMatchId(matchId);
        matchRepository.delete(match);
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public List<MatchOverviewResponse> getMatchOverview() {
        return matchRepository.findMatchOverview();
    }

    public List<Match> getMatchesBySport(String sport) {
        return matchRepository.findBySportIgnoreCase(sport);
    }

    public MatchAvailabilityResponse getMatchAvailability(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meciul nu exista"));

        int maxPlayers = match.getMaxPlayers();
        long registeredPlayers = participantRepository.countByMatchId(matchId);
        long availableSpots = Math.max(0L, maxPlayers - registeredPlayers);
        return new MatchAvailabilityResponse(maxPlayers, registeredPlayers, availableSpots);
    }

    private Match findOwnedMatchForUpdate(Long matchId, Long organizerId) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meciul nu exista"));
        if (!match.getCreator().getId().equals(organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar organizatorul poate schimba acest meci");
        }
        return match;
    }

    private SportsField findField(Long fieldId) {
        return sportsFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Terenul nu exista"));
    }
}
