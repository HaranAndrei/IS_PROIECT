package com.sportbuddy.sportbuddy.repository;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") Long matchId);
}
