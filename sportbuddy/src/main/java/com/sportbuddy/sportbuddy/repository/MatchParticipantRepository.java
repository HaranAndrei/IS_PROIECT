package com.sportbuddy.sportbuddy.repository;

import com.sportbuddy.sportbuddy.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchParticipantRepository
        extends JpaRepository<MatchParticipant, Long> {

    boolean existsByUserIdAndMatchId(Long userId, Long matchId);

    @Modifying
    @Query("""
            delete from MatchParticipant participant
            where participant.user.id = :userId and participant.match.id = :matchId
            """)
    int deleteByUserIdAndMatchId(@Param("userId") Long userId, @Param("matchId") Long matchId);

    @Modifying
    @Query("delete from MatchParticipant participant where participant.match.id = :matchId")
    void deleteByMatchId(@Param("matchId") Long matchId);

    List<MatchParticipant> findByMatchId(Long matchId);

    long countByMatchId(Long matchId);
}
