package com.prym.backend.service;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.User;
import com.prym.backend.repository.BuyerRepository;
import com.prym.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

// Handles all buyer profile business logic (creating, retrieving, and updating profiles)
// This layer enforces rules before anything touches the database
@Service
public class BuyerService {

    // We need both repositories: BuyerRepository to manage buyer profiles,
    // and UserRepository to look up the User account when linking a buyer to it
    private final BuyerRepository buyerRepository;
    private final UserRepository userRepository;

    // Spring automatically injects the repositories through this constructor
    public BuyerService(BuyerRepository buyerRepository, UserRepository userRepository) {
        this.buyerRepository = buyerRepository;
        this.userRepository = userRepository;
    }

    // Creates a new buyer profile linked to an existing user
    // Called when a buyer fills out the profile setup form after signing up
    public Buyer createBuyerProfile(Long userId, String firstName, String lastName,
                                     String phoneNumber, String preferredCuts, String quantity) {
        // Rule 1: The user must exist in the database before we can create a buyer profile
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Rule 2: A user can only have one buyer profile, so no duplicates allowed
        if (buyerRepository.existsByUserId(userId)) {
            throw new RuntimeException("Buyer profile already exists");
        }

        // All rules passed. Create the buyer profile and link it to the user
        Buyer buyer = new Buyer();
        buyer.setUser(user);
        buyer.setFirstName(firstName);
        buyer.setLastName(lastName);
        buyer.setPhoneNumber(phoneNumber);
        buyer.setPreferredCuts(preferredCuts);
        buyer.setQuantity(quantity);

        // Save to the database and return the created profile
        return buyerRepository.save(buyer);
    }

    // Retrieves the buyer profile for a given user
    // Called when a buyer navigates to their profile page
    public Buyer getBuyerProfile(Long userId) {
        return buyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));
    }

    // Updates an existing buyer's profile fields
    // Called when a buyer edits their profile and clicks save
    public Buyer updateBuyerProfile(Long userId, String firstName, String lastName,
                                     String phoneNumber, String preferredCuts, String quantity) {
        // Find the existing profile, can't update something that doesn't exist
        Buyer buyer = buyerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));

        // Overwrite the old values with the new ones
        buyer.setFirstName(firstName);
        buyer.setLastName(lastName);
        buyer.setPhoneNumber(phoneNumber);
        buyer.setPreferredCuts(preferredCuts);
        buyer.setQuantity(quantity);

        // Save the updated profile back to the database
        return buyerRepository.save(buyer);
    }
}
