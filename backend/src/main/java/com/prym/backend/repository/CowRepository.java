package com.prym.backend.repository;

import com.prym.backend.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//database access for individual Cow entities
@Repository
public interface CowRepository extends JpaRepository<Cow, Long> {
    //all cows of a specific type/breed
    List<Cow> findByCowTypeId(Long cowTypeId);
    //all cows belonging to a seller, traverses through the cowType relationship
    List<Cow> findByCowTypeSellerId(Long sellerId);
    //filter by seller AND status, handy for getting just the open ones
    List<Cow> findByCowTypeSellerIdAndStatus(Long sellerId, Cow.CowStatus status);
    //all cows with a certain status across all sellers
    List<Cow> findByStatus(Cow.CowStatus status);
}
