package com.sportbuddy.sportbuddy.repository;

import com.sportbuddy.sportbuddy.model.SportsField;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportsFieldRepository extends JpaRepository<SportsField, Long> {

    List<SportsField> findBySportIgnoreCase(String sport);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select field from SportsField field where field.id = :fieldId")
    Optional<SportsField> findByIdForUpdate(@Param("fieldId") Long fieldId);

    @Modifying
    @Query(value = "lock table sports_fields in share row exclusive mode", nativeQuery = true)
    void lockForCatalogImport();
}
