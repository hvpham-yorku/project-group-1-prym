package com.prym.backend.controller;
import com.prym.backend.model.CowType;
import com.prym.backend.service.CowTypeService;
import com.prym.backend.service.SessionService;
import java.util.Optional;
import com.prym.backend.model.User;
import com.prym.backend.model.Seller;
import com.prym.backend.service.SellerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//REST controller for seller profile operations.
//Uses session cookie auth instead of SecurityContext because seller endpoints
//are under /api/seller which has its own auth pattern.
@RestController
@RequestMapping("/api/seller")
public class SellerController {

  private final SellerService sellerService;
  private final SessionService sessionService;
  private final CowTypeService cowTypeService;

    public SellerController(SellerService sellerService, SessionService sessionService, CowTypeService cowTypeService) {
        this.sellerService = sellerService;
        this.sessionService = sessionService;
        this.cowTypeService = cowTypeService;
    }

    //creates a brand new seller profile, usually called right after registration
    @PostMapping("/profile")
    public ResponseEntity<?> createSeller(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.parseLong(request.get("userId"));
            String shopName = request.get("shopName");
            String shopAddress = request.get("shopAddress");
            String description = request.get("description");

            Seller seller = sellerService.createSellerProfile(userId, shopName, shopAddress, description);
            return ResponseEntity.ok(seller);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    //fetches the seller profile along with the user info, returns a flat DTO
    //with null-safe defaults so the frontend doesnt break on empty fields
    @GetMapping("/profile/{userId}")
public ResponseEntity<?> getSeller(@PathVariable Long userId,
        @CookieValue(name = "SESSION_ID", required = false) String sessionId) {
    
    // Verify the logged-in user matches the requested userId
    Optional<User> sessionUser = sessionService.validateSession(sessionId);
    if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
    }
    
    try {
        Seller seller = sellerService.getSellerProfile(userId);
        User user = seller.getUser();

        java.util.Map<String, Object> profileMap = new java.util.LinkedHashMap<>();
        profileMap.put("id", seller.getId());
        profileMap.put("firstName", user.getFirstName());
        profileMap.put("lastName", user.getLastName());
        profileMap.put("username", user.getUsername());
        profileMap.put("email", user.getEmail());
        profileMap.put("phoneNumber", user.getPhoneNumber());
        profileMap.put("shopName", seller.getShopName() == null ? "" : seller.getShopName());
        profileMap.put("shopAddress", seller.getShopAddress() == null ? "" : seller.getShopAddress());
        profileMap.put("description", seller.getDescription() == null ? "" : seller.getDescription());
        return ResponseEntity.ok(profileMap);
    } catch (RuntimeException e) {
        return ResponseEntity.status(404).body(Map.of("error", "Profile not found"));
    }
}
//updates seller profile fields, only changes what was actually sent in the request
@PatchMapping("/profile/{userId}")
        public ResponseEntity<?> updateSeller(@PathVariable Long userId,
        @RequestBody Map<String, String> request,
        @CookieValue(name = "SESSION_ID", required = false) String sessionId) {
    
    // Verify the logged-in user matches the requested userId
    Optional<User> sessionUser = sessionService.validateSession(sessionId);
    if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
    }
    
    try {
        String shopName = request.get("shopName");
        String phoneNumber = request.get("phoneNumber");
        String shopAddress = request.get("shopAddress");
        String description = request.get("description");

        Seller updatedSeller = sellerService.updateSellerProfile(userId, shopName, phoneNumber, shopAddress, description);
        return ResponseEntity.ok(updatedSeller);

    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

//returns every single farm/seller in the system, used by the farm listings page
//no auth check here because buyers need to see these too
@GetMapping("/all")
public ResponseEntity<?> getAllFarms() {
    try {
        List<Seller> farms = sellerService.getAllFarms();
        return ResponseEntity.ok(farms);
    } catch (RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

// returns a single farm by its seller profile id, public endpoint for the farm detail page
@GetMapping("/farm/{sellerId}")
public ResponseEntity<?> getFarmById(@PathVariable Long sellerId) {
    try {
        Seller seller = sellerService.getAllFarms().stream()
            .filter(s -> s.getId().equals(sellerId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Farm not found"));
        return ResponseEntity.ok(seller);
    } catch (RuntimeException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
}

// Public endpoint — buyers can see a farm's cow types without being logged in as that seller
@GetMapping("/{sellerId}/cow-types")
public ResponseEntity<?> getCowTypes(@PathVariable Long sellerId) {
    try {
        List<CowType> cowTypes = cowTypeService.getCowTypesBySellerProfileId(sellerId);
        return ResponseEntity.ok(cowTypes);
    } catch (RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
}
