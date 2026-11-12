package com.sportbuddy.sportbuddy.repository;

import com.sportbuddy.sportbuddy.model.SportsField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsFieldRepository extends JpaRepository<SportsField, Long> {

    List<SportsField> findBySportIgnoreCase(String sport);
}