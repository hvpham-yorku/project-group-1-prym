package com.prym.backend.service;

import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.SellerRepository;
import com.prym.backend.repository.UserRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Handles all seller profile business logic (creating, retrieving, and updating profiles)
// This layer enforces rules before anything touches the database
@Service
public class SellerService {
	
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    // Spring automatically injects the repositories through this constructor
    public SellerService(SellerRepository sellerRepository, UserRepository userRepository) {
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
    }

    // Creates a new seller profile linked to an existing user
    public Seller createSellerProfile(Long userId, String shopName, String shopAddress, String description) {
  
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

  
        if (sellerRepository.existsByUserId(userId)) {
            throw new RuntimeException("Seller profile already exists");
        }

     // Create seller profile and link to user
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName(shopName);
        seller.setShopAddress(shopAddress);
        seller.setDescription(description);

        return sellerRepository.save(seller);
    }

    // Retrieves the seller profile for a given user
    public Seller getSellerProfile(Long userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));
    }

    // Returns all seller profiles (used for the farm listings page)
    public List<Seller> getAllFarms() {
        return sellerRepository.findAll();
    }

    // Sets the average rating and total ratings directly (used by data initializer for dummy data)
    @Transactional
    public Seller setRating(Long userId, double averageRating, int totalRatings) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));
        seller.setAverageRating(averageRating);
        seller.setTotalRatings(totalRatings);
        return sellerRepository.save(seller);
    }

    // Updates an existing seller's shop info
    @Transactional
	public Seller updateSellerProfile(Long userId, String shopName, String phoneNumber, String shopAddress, String description) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        if (shopName != null) seller.setShopName(shopName);
        if (shopAddress != null) seller.setShopAddress(shopAddress);
        if (description != null) seller.setDescription(description);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            seller.getUser().setPhoneNumber(phoneNumber);
        }

        return sellerRepository.save(seller);
    }
    

}
