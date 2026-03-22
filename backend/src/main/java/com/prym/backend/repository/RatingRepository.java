package com.prym.backend.repository;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.Rating;
import com.prym.backend.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long>{
    List<Rating> findBySeller(Seller seller);
    boolean existsBySellerAndBuyer(Seller seller, Buyer buyer);
}
