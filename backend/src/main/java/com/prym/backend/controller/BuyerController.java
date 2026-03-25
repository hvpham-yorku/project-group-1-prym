package com.prym.backend.controller;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.User;
import com.prym.backend.service.BuyerService;
import com.prym.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import com.prym.backend.model.Seller;

// Handles all HTTP requests related to buyer profiles
// Base path is /api/buyer, which is already protected by SecurityConfig (only logged-in buyers can access)
@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final BuyerService buyerService;
    private final UserRepository userRepository;

    // Spring injects the BuyerService and UserRepository automatically through this constructor
    public BuyerController(BuyerService buyerService, UserRepository userRepository) {
        this.buyerService = buyerService;
        this.userRepository = userRepository;
    }

    // Gets the logged-in user's ID from the security context
    private Long getLoggedInUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // POST /api/buyer/profile, creates a new buyer profile after signup
    @PostMapping("/profile")
    public ResponseEntity<?> createProfile(@RequestBody Map<String, String> request) {
        try {
            // Extract fields from the JSON request body
            Long userId = Long.parseLong(request.get("userId"));

            if (!getLoggedInUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only create your own profile"));
            }

            String preferredCuts = request.get("preferredCuts");

            // Pass to the service, which checks the rules and saves it
            Buyer buyer = buyerService.createBuyerProfile(userId, preferredCuts);

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
            if (!getLoggedInUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only view your own profile"));
            }
            Buyer buyer = buyerService.getBuyerProfile(userId);
            return ResponseEntity.ok(buyer);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/buyer/profile/{userId}, updates an existing buyer's profile
@PatchMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            if (!getLoggedInUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only edit your own profile"));
            }
            // Extract the updated fields from the JSON request body
            String preferredCuts = request.get("preferredCuts");
            String phoneNumber = request.get("phoneNumber");
            // Pass to the service, which finds the existing profile and updates it
            Buyer buyer = buyerService.updateBuyerProfile(userId, preferredCuts, phoneNumber);

            return ResponseEntity.ok(buyer);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


	// return list of saved farms
	@GetMapping("/all")
	public ResponseEntity<?> getSavedFarms(){
		try {
			Long userId = getLoggedInUserId();
			List<Seller> savedFarms = buyerService.getSavedFarms(userId);
			return ResponseEntity.ok(savedFarms);
		} catch (RuntimeException e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	@PatchMapping("/all")
	public ResponseEntity<?> saveFarm(@RequestBody Seller farm){
		try {
			Long userId = getLoggedInUserId();
			Buyer buyer = buyerService.getBuyerProfile(userId);
			if(!buyer.getSavedFarms().contains(farm)) {
				buyer = buyerService.saveFarm(userId, farm);
			}
			return ResponseEntity.ok(buyer);
		} catch (RuntimeException e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}
	
	@DeleteMapping("/all/{farm}")
	public ResponseEntity<?> removeSavedFarm(@RequestBody Seller farm){
		try {
			Long userId = getLoggedInUserId();
			Buyer buyer = buyerService.getBuyerProfile(userId);
			if(buyer.getSavedFarms().contains(farm)) {
				buyer = buyerService.removeSavedFarm(userId, farm);
			}
			return ResponseEntity.ok(buyer);
		} catch (RuntimeException e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}
}
