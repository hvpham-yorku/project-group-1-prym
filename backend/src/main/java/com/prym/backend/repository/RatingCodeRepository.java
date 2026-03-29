package com.prym.backend.repository;

import com.prym.backend.model.RatingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

//database access for one-time rating codes that sellers generate
public interface RatingCodeRepository extends JpaRepository<RatingCode, Long> {
    //look up a code by its string value
    Optional<RatingCode> findByCode(String code);

    //atomically marks a code as used. Returns 1 if it worked, 0 if the code
    //doesnt exist or was already used. This prevents race conditions where two
    //buyers try to redeem the same code at the same time.
    @Modifying
    @Query("UPDATE RatingCode rc SET rc.used = true WHERE rc.code = :code AND rc.used = false")
    int markAsUsed(@Param("code") String code);
}