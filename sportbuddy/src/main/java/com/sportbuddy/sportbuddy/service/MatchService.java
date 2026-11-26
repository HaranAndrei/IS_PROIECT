package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.dto.CreateMatchRequest;
import com.sportbuddy.sportbuddy.dto.MatchAvailabilityResponse;
import com.sportbuddy.sportbuddy.dto.MatchOverviewResponse;
import com.sportbuddy.sportbuddy.dto.UpdateMatchRequest;
import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.model.MatchParticipant;
import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.MatchParticipantRepository;
import com.sportbuddy.sportbuddy.repository.MatchRepository;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final MatchReservationService reservationService;
    private final MatchParticipantRepository participantRepository;

    public MatchService(MatchRepository matchRepository,
                        UserRepository userRepository,
                        MatchReservationService reservationService,
                        MatchParticipantRepository participantRepository) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public Match createMatch(Long organizerId, CreateMatchRequest request) {
        User creator = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organizatorul nu exista"));
        int durationHours = Objects.requireNonNullElse(request.durationHours(), Match.DEFAULT_DURATION_HOURS);
        SportsField field = reservationService.reserve(request.field().id(), request.startTime(), durationHours, null);

        int playersNeeded = Objects.requireNonNullElse(request.playersNeeded(), 0);
        Match match = new Match(request.sport(), request.startTime(), request.maxPlayers(),
                playersNeeded, creator, field);
        match.setDurationHours(durationHours);
        Match savedMatch = matchRepository.save(match);
        participantRepository.save(new MatchParticipant(creator, savedMatch));
        return savedMatch;
    }

    @Transactional
    public Match updateMatch(Long matchId, Long organizerId, UpdateMatchRequest request) {
        Match match = findOwnedMatchForUpdate(matchId, organizerId);
        if (participantRepository.countByMatchId(matchId) > request.maxPlayers()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Capacitatea este mai mica decat numarul de jucatori inscrisi");
        }

        int durationHours = Objects.requireNonNullElse(request.durationHours(), match.getDurationHours());
        if (!match.getField().getId().equals(request.fieldId())
                || !match.getStartTime().equals(request.startTime())
                || match.getDurationHours() != durationHours) {
            SportsField field = reservationService.reserve(request.fieldId(), request.startTime(), durationHours, matchId);
            match.setField(field);
            match.setStartTime(request.startTime());
            match.setDurationHours(durationHours);
        }
        match.setSport(request.sport());
        match.setMaxPlayers(request.maxPlayers());
        match.setPlayersNeeded(request.playersNeeded());
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

    public List<MatchOverviewResponse> getAvailableMatches(Long userId) {
        return matchRepository.findAvailableMatches(userId, LocalDateTime.now(ClujCourtCatalog.ZONE));
    }

    public List<LocalTime> getAvailableSlots(Long organizerId, Long fieldId, LocalDate date,
                                             int durationHours, Long editedMatchId) {
        if (editedMatchId != null) {
            requireOrganizer(findMatch(editedMatchId), organizerId);
        }
        return reservationService.availableSlots(fieldId, date, durationHours, editedMatchId);
    }

    public List<Match> getMatchesBySport(String sport) {
        return matchRepository.findBySportIgnoreCase(sport);
    }

    public MatchAvailabilityResponse getMatchAvailability(Long matchId) {
        Match match = findMatch(matchId);

        int maxPlayers = match.getMaxPlayers();
        long registeredPlayers = participantRepository.countByMatchId(matchId);
        long availableSpots = Math.max(0L, maxPlayers - registeredPlayers);
        return new MatchAvailabilityResponse(maxPlayers, registeredPlayers, availableSpots);
    }

    private Match findOwnedMatchForUpdate(Long matchId, Long organizerId) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meciul nu exista"));
        requireOrganizer(match, organizerId);
        return match;
    }

    private static void requireOrganizer(Match match, Long organizerId) {
        if (!match.getCreator().getId().equals(organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar organizatorul poate schimba acest meci");
        }
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meciul nu exista"));
    }
}
