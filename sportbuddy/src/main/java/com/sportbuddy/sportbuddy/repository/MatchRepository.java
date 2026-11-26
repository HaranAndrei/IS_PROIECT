package com.sportbuddy.sportbuddy.repository;

import com.sportbuddy.sportbuddy.dto.MatchOverviewResponse;
import com.sportbuddy.sportbuddy.model.Match;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    String OVERVIEW_SELECT = """
            select new com.sportbuddy.sportbuddy.dto.MatchOverviewResponse(
                m.id, m.sport, field.id, field.name, m.creator.id, m.startTime,
                coalesce(m.durationHours, 1), m.maxPlayers, m.playersNeeded,
                greatest(0L, m.maxPlayers - count(participant.id))
            )
            from Match m
            join m.field field
            left join MatchParticipant participant on participant.match = m
            """;

    String OVERVIEW_GROUP = """
            group by m.id, m.sport, field.id, field.name, m.creator.id, m.startTime,
                m.durationHours, m.maxPlayers, m.playersNeeded
            """;

    String OVERVIEW_ORDER = "order by m.startTime, m.id";

    List<Match> findBySportIgnoreCase(String sport);

    List<Match> findByFieldIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
            Long fieldId, LocalDateTime from, LocalDateTime until);

    @Query(OVERVIEW_SELECT + OVERVIEW_GROUP + OVERVIEW_ORDER)
    List<MatchOverviewResponse> findMatchOverview();

    @Query(OVERVIEW_SELECT + """
            where m.startTime > :now
                and not exists (
                    select 1 from MatchParticipant registration
                    where registration.match = m and registration.user.id = :userId
                )
            """ + OVERVIEW_GROUP + "having count(participant.id) < m.maxPlayers " + OVERVIEW_ORDER)
    List<MatchOverviewResponse> findAvailableMatches(@Param("userId") Long userId,
                                                     @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") Long matchId);
}
