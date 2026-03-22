package com.prym.backend.repository;

import com.prym.backend.model.RatingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RatingCodeRepository extends JpaRepository<RatingCode, Long> {
    Optional<RatingCode> findByCode(String code);

    @Modifying
    @Query("UPDATE RatingCode rc SET rc.used = true WHERE rc.code = :code AND rc.used = false")
    int markAsUsed(@Param("code") String code);
}