package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.model.MatchParticipant;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.MatchParticipantRepository;
import com.sportbuddy.sportbuddy.repository.MatchRepository;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.stereotype.Service;

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

    public MatchParticipant joinMatch(Long userId, Long matchId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilizatorul nu exista: " + userId));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Meciul nu exista: " + matchId));

        if (participantRepository.existsByUserIdAndMatchId(userId, matchId)) {
            throw new IllegalStateException(
                    "Utilizatorul este deja inscris la acest meci");
        }

        long currentParticipants = participantRepository.countByMatchId(matchId);

        if (currentParticipants >= match.getMaxPlayers()) {
            throw new IllegalStateException(
                    "Meciul nu mai are locuri disponibile");
        }

        MatchParticipant participant = new MatchParticipant(user, match);
        return participantRepository.save(participant);
    }

    public List<MatchParticipant> getParticipants(Long matchId) {
        return participantRepository.findByMatchId(matchId);
    }
}