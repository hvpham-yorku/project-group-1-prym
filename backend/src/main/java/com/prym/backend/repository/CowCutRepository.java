package com.prym.backend.repository;

import com.prym.backend.model.CowCut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//database access for individual cuts of a cow (the 22 records per cow)
@Repository
public interface CowCutRepository extends JpaRepository<CowCut, Long> {
    //get all 22 cuts for a cow
    List<CowCut> findByCowId(Long cowId);
    //get cuts filtered by status, usually to find just the AVAILABLE ones
    List<CowCut> findByCowIdAndStatus(Long cowId, CowCut.CutStatus status);
}
