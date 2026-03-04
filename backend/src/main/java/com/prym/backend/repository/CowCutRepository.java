package com.prym.backend.repository;

import com.prym.backend.model.CowCut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CowCutRepository extends JpaRepository<CowCut, Long> {
    List<CowCut> findByCowId(Long cowId);
    List<CowCut> findByCowIdAndStatus(Long cowId, CowCut.CutStatus status);
}
