package com.prym.backend.repository;

import com.prym.backend.model.BuyerMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerMatchRepository extends JpaRepository<BuyerMatch, Long> {
    List<BuyerMatch> findByCowId(Long cowId);
    List<BuyerMatch> findByBuyerId(Long buyerId);
    List<BuyerMatch> findByCowIdAndStatus(Long cowId, BuyerMatch.MatchStatus status);
    boolean existsByCowIdAndBuyerId(Long cowId, Long buyerId);
    Optional<BuyerMatch> findByCowIdAndBuyerId(Long cowId, Long buyerId);
}
