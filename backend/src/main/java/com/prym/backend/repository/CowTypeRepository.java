package com.prym.backend.repository;

import com.prym.backend.model.CowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CowTypeRepository extends JpaRepository<CowType, Long> {
    List<CowType> findBySellerId(Long sellerId);
}
