package com.prym.backend.controller;

import com.prym.backend.model.Seller;
import com.prym.backend.service.SellerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> createSeller(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.parseLong(request.get("userId"));
            String shopName = request.get("shopName");
            String shopAddress = request.get("shopAddress");

            Seller seller = sellerService.createSellerProfile(userId, shopName, shopAddress);
            return ResponseEntity.ok(seller);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getSeller(@PathVariable Long userId) {
        try {
            Seller seller = sellerService.getSellerProfile(userId);
            return ResponseEntity.ok(seller);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<?> updateSeller(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String shopName = request.get("shopName");
            String shopAddress = request.get("shopAddress");

            Seller updatedSeller = sellerService.updateSellerProfile(userId, shopName, shopAddress);
            return ResponseEntity.ok(updatedSeller);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
