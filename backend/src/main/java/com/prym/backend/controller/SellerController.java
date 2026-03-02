package com.prym.backend.controller;
import com.prym.backend.service.SessionService;
import java.util.Optional;
import com.prym.backend.model.User;
import com.prym.backend.model.Seller;
import com.prym.backend.service.SellerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

  private final SellerService sellerService;
private final SessionService sessionService;

public SellerController(SellerService sellerService, SessionService sessionService) {
    this.sellerService = sellerService;
    this.sessionService = sessionService;
}

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

        return ResponseEntity.ok(Map.of(
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "phoneNumber", user.getPhoneNumber(),
            "shopName", seller.getShopName() == null ? "" : seller.getShopName(),
            "shopAddress", seller.getShopAddress() == null ? "" : seller.getShopAddress(),

            "category",seller.getCategory() == null ? "" : seller.getCategory().name(),
            "description", seller.getDescription() == null ? "" : seller.getDescription()
        ));
    } catch (RuntimeException e) {
        return ResponseEntity.status(404).body(Map.of("error", "Profile not found"));
    }
}
@PatchMapping("/{userId}")
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
        String shopAddress = request.get("shopAddress");
        String description = request.get("description");
        String category = request.get("category");

        Seller updatedSeller = sellerService.updateSellerProfile(userId, shopName, shopAddress, description, category);
        return ResponseEntity.ok(updatedSeller);

    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
}
