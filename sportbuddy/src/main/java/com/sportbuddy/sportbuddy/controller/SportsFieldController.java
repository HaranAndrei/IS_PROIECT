package com.sportbuddy.sportbuddy.controller;

import com.sportbuddy.sportbuddy.model.SportsField;
import com.sportbuddy.sportbuddy.service.SportsFieldService;
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
    public SportsField createField(@RequestBody SportsField sportsField) {
        return sportsFieldService.createField(sportsField);
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