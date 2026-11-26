package com.sportbuddy.sportbuddy.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.repository.SportsFieldRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ClujCourtCatalog implements ApplicationRunner {

    static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");

    private final SportsFieldRepository sportsFieldRepository;
    private final List<Court> courts;
    private final Map<FieldKey, Court> courtsByField;

    public ClujCourtCatalog(JsonMapper jsonMapper, SportsFieldRepository sportsFieldRepository) throws IOException {
        this.sportsFieldRepository = sportsFieldRepository;
        try (var source = new ClassPathResource("static/cluj-courts.json").getInputStream()) {
            courts = List.of(jsonMapper.readValue(source, Court[].class));
        }
        Map<FieldKey, Court> byField = new HashMap<>();
        for (Court court : courts) {
            for (String sport : court.sports()) {
                byField.put(FieldKey.of(court.name(), sport), court);
            }
        }
        courtsByField = Map.copyOf(byField);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sportsFieldRepository.lockForCatalogImport();
        Set<FieldKey> existingFields = new HashSet<>();
        for (SportsField field : sportsFieldRepository.findAll()) {
            existingFields.add(FieldKey.of(field.getName(), field.getSport()));
        }
        for (Court court : courts) {
            for (String sport : court.sports()) {
                if (existingFields.add(FieldKey.of(court.name(), sport))) {
                    sportsFieldRepository.save(new SportsField(
                            court.name(), sport, court.address(), court.latitude(), court.longitude()));
                }
            }
        }
    }

    public Optional<OpeningWindow> openingWindow(SportsField field, LocalDate date) {
        Court court = courtsByField.get(FieldKey.of(field.getName(), field.getSport()));
        if (court == null) return Optional.empty();
        boolean summer = ZONE.getRules().isDaylightSavings(date.atTime(12, 0).atZone(ZONE).toInstant());
        return court.openingHours().stream()
                .filter(rule -> rule.appliesTo(date.getDayOfWeek(), summer))
                .map(rule -> new OpeningWindow(rule.opensAt(), rule.closesAt()))
                .findFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Court(String name, String address, double latitude, double longitude, List<String> sports,
                         List<OpeningRule> openingHours) {
    }

    private record OpeningRule(List<DayOfWeek> days, Boolean summer, LocalTime opensAt, LocalTime closesAt) {
        boolean appliesTo(DayOfWeek day, boolean isSummer) {
            return (days == null || days.contains(day)) && (summer == null || summer == isSummer);
        }
    }

    public record OpeningWindow(LocalTime opensAt, LocalTime closesAt) {
        public boolean contains(LocalDateTime start, int durationHours) {
            return !start.toLocalTime().isBefore(opensAt)
                    && !start.plusHours(durationHours).isAfter(start.toLocalDate().atTime(closesAt));
        }
    }

    private record FieldKey(String name, String sport) {
        private static FieldKey of(String name, String sport) {
            return new FieldKey(normalize(name), normalize(sport));
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
