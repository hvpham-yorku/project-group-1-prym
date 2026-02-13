package com.prym.backend.repository;

import com.prym.backend.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    // Finds a seller profile by their User ID
    Optional<Seller> findByUserId(Long userId);

    // Checks if a seller profile already exists for a given User. Prevents duplicates
    boolean existsByUserId(Long userId);
}
