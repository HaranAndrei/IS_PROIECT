package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.model.Match;
import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.repository.MatchRepository;
import com.sportbuddy.sportbuddy.repository.SportsFieldRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MatchReservationService {

    private static final int MAX_DURATION_HOURS = 2;

    private final MatchRepository matchRepository;
    private final SportsFieldRepository fieldRepository;
    private final ClujCourtCatalog courtCatalog;

    public MatchReservationService(MatchRepository matchRepository, SportsFieldRepository fieldRepository,
                                   ClujCourtCatalog courtCatalog) {
        this.matchRepository = matchRepository;
        this.fieldRepository = fieldRepository;
        this.courtCatalog = courtCatalog;
    }

    public List<LocalTime> availableSlots(Long fieldId, LocalDate date, int durationHours, Long editedMatchId) {
        SportsField field = requireField(fieldRepository.findById(fieldId));
        var openingWindow = courtCatalog.openingWindow(field, date);
        if (openingWindow.isEmpty()) return List.of();

        var window = openingWindow.get();
        LocalDateTime now = LocalDateTime.now(ClujCourtCatalog.ZONE);
        List<Match> bookings = fieldBookings(fieldId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        List<LocalTime> slots = new ArrayList<>();
        for (int hour = 0; hour + durationHours <= 24; hour++) {
            LocalDateTime start = date.atTime(hour, 0);
            if (start.isAfter(now) && window.contains(start, durationHours)
                    && isFree(bookings, start, durationHours, editedMatchId)) {
                slots.add(start.toLocalTime());
            }
        }
        return slots;
    }

    // Call inside the transaction that saves the match so competing requests wait for this reservation.
    public SportsField reserve(Long fieldId, LocalDateTime start, int durationHours, Long editedMatchId) {
        if (!start.isAfter(LocalDateTime.now(ClujCourtCatalog.ZONE))
                || !start.toLocalTime().equals(LocalTime.of(start.getHour(), 0))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alege o ora fixa, in viitor");
        }
        SportsField field = requireField(fieldRepository.findByIdForUpdate(fieldId));
        var window = courtCatalog.openingWindow(field, start.toLocalDate())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Terenul nu are program de rezervare configurat pentru aceasta zi"));
        if (!window.contains(start, durationHours)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intervalul depaseste programul terenului");
        }
        List<Match> bookings = fieldBookings(fieldId, start, start.plusHours(durationHours));
        if (!isFree(bookings, start, durationHours, editedMatchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Terenul este deja rezervat in acest interval");
        }
        return field;
    }

    private static SportsField requireField(Optional<SportsField> field) {
        return field.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Terenul nu exista"));
    }

    private List<Match> fieldBookings(Long fieldId, LocalDateTime start, LocalDateTime end) {
        return matchRepository.findByFieldIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                fieldId, start.minusHours(MAX_DURATION_HOURS), end);
    }

    private static boolean isFree(List<Match> bookings, LocalDateTime start, int duration, Long ignoredMatchId) {
        LocalDateTime end = start.plusHours(duration);
        return bookings.stream().noneMatch(match -> !match.getId().equals(ignoredMatchId)
                && match.getStartTime().isBefore(end)
                && match.getStartTime().plusHours(match.getDurationHours()).isAfter(start));
    }
}
