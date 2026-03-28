package com.prym.backend.repository;

import com.prym.backend.model.BuyerMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//database access for buyer-to-cow matching records
@Repository
public interface BuyerMatchRepository extends JpaRepository<BuyerMatch, Long> {
    //all buyers matched to a specific cow
    List<BuyerMatch> findByCowId(Long cowId);
    //all cows a buyer has been matched to
    List<BuyerMatch> findByBuyerId(Long buyerId);
    //matches for a cow filtered by status (PENDING, CONFIRMED, etc)
    List<BuyerMatch> findByCowIdAndStatus(Long cowId, BuyerMatch.MatchStatus status);
    //check if a buyer is already matched to a cow, prevents duplicates
    boolean existsByCowIdAndBuyerId(Long cowId, Long buyerId);
    //find the specific match record between a buyer and cow
    Optional<BuyerMatch> findByCowIdAndBuyerId(Long cowId, Long buyerId);
}
