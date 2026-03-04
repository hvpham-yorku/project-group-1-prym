package com.prym.backend.repository;

import com.prym.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CowRepository extends JpaRepository<Cow, Long> {
    List<Cow> findByCowTypeId(Long cowTypeId);
    List<Cow> findByCowTypeSellerId(Long sellerId);
    List<Cow> findByCowTypeSellerIdAndStatus(Long sellerId, Cow.CowStatus status);
}
