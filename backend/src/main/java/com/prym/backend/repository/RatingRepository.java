package com.prym.backend.repository;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.Rating;
import com.prym.backend.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

//database access for buyer-submitted ratings of farms
public interface RatingRepository extends JpaRepository<Rating, Long>{
    //get all ratings for a particular seller/farm
    List<Rating> findBySeller(Seller seller);
    //check if this buyer already rated this seller, each buyer can only rate once per farm
    boolean existsBySellerAndBuyer(Seller seller, Buyer buyer);
}
