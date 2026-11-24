package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.dto.CreateSportsFieldRequest;
import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.repository.SportsFieldRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SportsFieldService {

    private final SportsFieldRepository sportsFieldRepository;

    public SportsFieldService(SportsFieldRepository sportsFieldRepository) {
        this.sportsFieldRepository = sportsFieldRepository;
    }

    public SportsField createField(CreateSportsFieldRequest request) {
        SportsField sportsField = new SportsField(request.name(), request.sport(), request.address(),
                request.latitude().doubleValue(), request.longitude().doubleValue());
        return sportsFieldRepository.save(sportsField);
    }

    public List<SportsField> getAllFields() {
        return sportsFieldRepository.findAll();
    }

    public List<SportsField> getFieldsBySport(String sport) {
        return sportsFieldRepository.findBySportIgnoreCase(sport);
    }
}
