package com.sportbuddy.sportbuddy.repository;

import com.sportbuddy.sportbuddy.dto.MatchOverviewResponse;
import com.sportbuddy.sportbuddy.model.Match;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findBySportIgnoreCase(String sport);

    @Query("""
            select new com.sportbuddy.sportbuddy.dto.MatchOverviewResponse(
                m.id, m.sport, field.name, m.startTime, m.maxPlayers,
                greatest(0L, m.maxPlayers - count(participant.id))
            )
            from Match m
            join m.field field
            left join MatchParticipant participant on participant.match = m
            group by m.id, m.sport, field.name, m.startTime, m.maxPlayers
            order by m.startTime, m.id
            """)
    List<MatchOverviewResponse> findMatchOverview();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") Long matchId);
}
