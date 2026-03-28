package com.prym.backend.controller;

import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

//Rating endpoints — sellers generate codes, buyers use them to submit ratings.
//The generate endpoint is for sellers, the submit endpoint is for buyers,
//and the GET endpoint is public so anyone can see a farm's ratings.
@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final RatingService ratingService;
    private final UserRepository userRepository;

    public RatingController(RatingService ratingService, UserRepository userRepository) {
        this.ratingService = ratingService;
        this.userRepository = userRepository;
    }

    //grabs the logged in user's id from the security context
    private Long getLoggedInUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    // after transaction, seller calls this to get a one time code
    @PostMapping("/generate-code")
    public ResponseEntity<?> generateCode(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(ratingService.generateRatingCode(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Buyer submits a rating using the code the seller gave them
    @PostMapping("/submit")
    public ResponseEntity<?> submitRating(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            String code = (String) body.get("code");
            int score = Integer.parseInt(body.get("score").toString());
            return ResponseEntity.ok(ratingService.submitRating(userId, code, score));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // anyone can view a farm's ratings
    @GetMapping("/{sellerUsername}")
    public ResponseEntity<?> getFarmRatings(@PathVariable String sellerUsername) {
        try {
            return ResponseEntity.ok(ratingService.getFarmRatings(sellerUsername));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}