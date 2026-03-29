package com.prym.backend.unit.controller;

import com.prym.backend.controller.RatingController;
import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RatingController.
 * Security context is manually set to simulate logged-in users.
 */
public class RatingControllerTest {

    @Mock
    private RatingService ratingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RatingController ratingController;

    private User sellerUser;
    private User buyerUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sellerUser = new User();
        sellerUser.setId(1L);
        sellerUser.setEmail("seller@example.com");
        sellerUser.setRole(User.Role.SELLER);

        buyerUser = new User();
        buyerUser.setId(2L);
        buyerUser.setEmail("buyer@example.com");
        buyerUser.setRole(User.Role.BUYER);
    }

    private void loginAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }


    // ─── getFarmRatings ────────────────────────────────────────────────

    // Test 14: getFarmRatings_Success — any user can view → 200 with shop/rating data
    @Test
    void getFarmRatings_Success_Returns200WithRatingData() {
        Map<String, Object> ratingData = Map.of(
                "shopName",      "Green Farm",
                "averageRating", 4.2,
                "totalRatings",  5,
                "ratings",       List.of(Map.of("score", 4), Map.of("score", 5))
        );
        when(ratingService.getFarmRatings("greenfarmer")).thenReturn(ratingData);

        ResponseEntity<?> response = ratingController.getFarmRatings("greenfarmer");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertNotNull(result);
        assertEquals("Green Farm",  result.get("shopName"));
        assertEquals(4.2,           (double) result.get("averageRating"), 0.001);
        assertEquals(5,             result.get("totalRatings"));
        assertNotNull(result.get("ratings"));
    }

    // Test 15: getFarmRatings_SellerNotFound — service throws → 400
    @Test
    void getFarmRatings_SellerNotFound_Returns400() {
        when(ratingService.getFarmRatings("nobody"))
                .thenThrow(new RuntimeException("Seller not found"));

        ResponseEntity<?> response = ratingController.getFarmRatings("nobody");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Seller not found", result.get("error"));
    }

    // Test 16: getFarmRatings_NoRatings — seller exists with zero ratings → 200 with empty ratings list
    @Test
    void getFarmRatings_ZeroRatings_Returns200WithEmptyList() {
        Map<String, Object> ratingData = Map.of(
                "shopName",      "New Farm",
                "averageRating", 0.0,
                "totalRatings",  0,
                "ratings",       List.of()
        );
        when(ratingService.getFarmRatings("newfarm")).thenReturn(ratingData);

        ResponseEntity<?> response = ratingController.getFarmRatings("newfarm");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(0, result.get("totalRatings"));
        assertTrue(((List<?>) result.get("ratings")).isEmpty());
    }

    // Test 17: getFarmRatings_IsPublic — no auth setup needed, endpoint is publicly accessible
    @Test
    void getFarmRatings_NoAuthRequired_ReturnsData() {
        // No loginAs() call — verifies endpoint does not require authentication
        when(ratingService.getFarmRatings("publicfarm"))
                .thenReturn(Map.of("shopName", "Public Farm", "averageRating", 3.5,
                        "totalRatings", 2, "ratings", List.of()));

        ResponseEntity<?> response = ratingController.getFarmRatings("publicfarm");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

  
}