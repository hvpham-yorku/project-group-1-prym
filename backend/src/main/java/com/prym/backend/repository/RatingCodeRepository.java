package com.prym.backend.repository;

import com.prym.backend.model.RatingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RatingCodeRepository extends JpaRepository<RatingCode, Long> {
    Optional<RatingCode> findByCode(String code);
}