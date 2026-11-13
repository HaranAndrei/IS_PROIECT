package com.sportbuddy.sportbuddy.repository;

import com.sportbuddy.sportbuddy.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantRepository
        extends JpaRepository<MatchParticipant, Long> {

    boolean existsByUserIdAndMatchId(Long userId, Long matchId);

    List<MatchParticipant> findByMatchId(Long matchId);

    long countByMatchId(Long matchId);
}