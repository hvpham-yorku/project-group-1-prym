package com.prym.backend.service;

import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.SellerRepository;
import com.prym.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // Updates an existing seller's shop info
    @Transactional
	public Seller updateSellerProfile(Long userId, String shopName, String phoneNumber, String shopAddress, String description, String category) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        seller.setShopName(shopName);
        seller.setShopAddress(shopAddress);
        seller.setDescription(description);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            User user = seller.getUser();
            user.setPhoneNumber(phoneNumber);
            userRepository.save(user);
        }

        return sellerRepository.save(seller);
    }
}
