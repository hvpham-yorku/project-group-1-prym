package com.prym.backend.controller;

import com.prym.backend.model.Buyer;
import com.prym.backend.service.BuyerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Handles all HTTP requests related to buyer profiles
// Base path is /api/buyer, which is already protected by SecurityConfig (only logged-in buyers can access)
@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final BuyerService buyerService;

    // Spring injects the BuyerService automatically through this constructor
    public BuyerController(BuyerService buyerService) {
        this.buyerService = buyerService;
    }

    // POST /api/buyer/profile, creates a new buyer profile after signup
    @PostMapping("/profile")
    public ResponseEntity<?> createProfile(@RequestBody Map<String, String> request) {
        try {
            // Extract fields from the JSON request body
            Long userId = Long.parseLong(request.get("userId"));
            String preferredCuts = request.get("preferredCuts");
            String quantity = request.get("quantity");

            // Pass to the service, which checks the rules and saves it
            Buyer buyer = buyerService.createBuyerProfile(userId, preferredCuts, quantity);

            return ResponseEntity.ok(buyer);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/profile/{userId}, retrieves a buyer's profile
    // The userId comes from the URL itself (e.g., /api/buyer/profile/5 means userId = 5)
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        try {
            Buyer buyer = buyerService.getBuyerProfile(userId);
            return ResponseEntity.ok(buyer);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/buyer/profile/{userId}, updates an existing buyer's profile
    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            // Extract the updated fields from the JSON request body
            String preferredCuts = request.get("preferredCuts");
            String quantity = request.get("quantity");

            // Pass to the service, which finds the existing profile and updates it
            Buyer buyer = buyerService.updateBuyerProfile(userId, preferredCuts, quantity);

            return ResponseEntity.ok(buyer);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
