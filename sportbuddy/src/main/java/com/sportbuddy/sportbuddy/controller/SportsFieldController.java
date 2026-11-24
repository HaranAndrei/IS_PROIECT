package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.dto.CreateSportsFieldRequest;
import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.service.SportsFieldService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
public class SportsFieldController {

    private final SportsFieldService sportsFieldService;

    public SportsFieldController(SportsFieldService sportsFieldService) {
        this.sportsFieldService = sportsFieldService;
    }

    @PostMapping
    public SportsField createField(@Valid @RequestBody CreateSportsFieldRequest request) {
        return sportsFieldService.createField(request);
    }

    @GetMapping
    public List<SportsField> getAllFields() {
        return sportsFieldService.getAllFields();
    }

    @GetMapping("/sport/{sport}")
    public List<SportsField> getFieldsBySport(@PathVariable String sport) {
        return sportsFieldService.getFieldsBySport(sport);
    }
}
