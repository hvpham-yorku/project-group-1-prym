package com.prym.backend.repository;

import com.prym.backend.model.Seller;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    // Finds a seller profile by their User ID
    Optional<Seller> findByUserId(Long userId);

    // Checks if a seller profile already exists for a given User. Prevents duplicates
    boolean existsByUserId(Long userId);

    // Checks if a seller profile already exists for a given username of the Seller
    Optional<Seller> findByUserUsername(String username);

    // Fetches a seller with a pessimistic write lock to prevent lost-update races
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seller s WHERE s.id = :id")
    Optional<Seller> findByIdForUpdate(@Param("id") Long id);
}
