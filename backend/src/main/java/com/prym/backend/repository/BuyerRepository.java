package com.prym.backend.repository;

import com.prym.backend.model.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    // Finds a buyer profile by their User ID
    Optional<Buyer> findByUserId(Long userId);

    // Checks if a buyer profile already exists for a given User. Used to prevent duplicate profiles
    boolean existsByUserId(Long userId);
}
