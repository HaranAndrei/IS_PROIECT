package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.model.MatchParticipant;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.MatchParticipantRepository;
import com.sportbuddy.sportbuddy.repository.MatchRepository;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchParticipantService {

    private final MatchParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    public MatchParticipantService(
            MatchParticipantRepository participantRepository,
            UserRepository userRepository,
            MatchRepository matchRepository) {

        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public MatchParticipant joinMatch(Long userId, Long matchId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilizatorul nu exista: " + userId));

        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Meciul nu exista: " + matchId));

        if (!match.getStartTime().isAfter(LocalDateTime.now(ClujCourtCatalog.ZONE))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Meciul a inceput deja");
        }

        if (participantRepository.existsByUserIdAndMatchId(userId, matchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Utilizatorul este deja inscris la acest meci");
        }

        long currentParticipants = participantRepository.countByMatchId(matchId);

        if (currentParticipants >= match.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Meciul nu mai are locuri disponibile");
        }

        MatchParticipant participant = new MatchParticipant(user, match);
        return participantRepository.save(participant);
    }

    @Transactional
    public boolean leaveMatch(Long userId, Long matchId) {
        return participantRepository.deleteByUserIdAndMatchId(userId, matchId) > 0;
    }

    public List<MatchParticipant> getParticipants(Long matchId) {
        if (!matchRepository.existsById(matchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meciul nu exista: " + matchId);
        }
        return participantRepository.findByMatchId(matchId);
    }
}
