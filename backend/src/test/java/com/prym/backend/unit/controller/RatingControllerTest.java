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

    // ─── generateCode ─────────────────────────────────────────────────

    // Test 1: generateCode_Success — seller calls with their own userId → 200 with "PRYM-" code
    @Test
    void generateCode_Success_Returns200WithCode() {
        loginAs(sellerUser);
        Map<String, Object> body = Map.of("userId", 1);
        when(ratingService.generateRatingCode(1L))
                .thenReturn(Map.of("code", "PRYM-ABC123"));

        ResponseEntity<?> response = ratingController.generateCode(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertNotNull(result);
        assertEquals("PRYM-ABC123", result.get("code"));
    }

    // Test 2: generateCode_Forbidden — userId in body doesn't match logged-in user → 403
    @Test
    void generateCode_Forbidden_Returns403() {
        loginAs(sellerUser); // logged in as seller (id=1)
        Map<String, Object> body = Map.of("userId", 99); // different ID

        ResponseEntity<?> response = ratingController.generateCode(body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", result.get("error"));
        verify(ratingService, never()).generateRatingCode(any());
    }

    // Test 3: generateCode_SellerNotFound — service throws "Seller not found" → 400
    @Test
    void generateCode_SellerNotFound_Returns400() {
        loginAs(sellerUser);
        Map<String, Object> body = Map.of("userId", 1);
        when(ratingService.generateRatingCode(1L))
                .thenThrow(new RuntimeException("Seller not found"));

        ResponseEntity<?> response = ratingController.generateCode(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Seller not found", result.get("error"));
    }

    // Test 4: generateCode_CodeStartsWithPRYM — format check (via mock)
    @Test
    void generateCode_CodeHasCorrectFormat() {
        loginAs(sellerUser);
        Map<String, Object> body = Map.of("userId", 1);
        when(ratingService.generateRatingCode(1L))
                .thenReturn(Map.of("code", "PRYM-XY1234"));

        ResponseEntity<?> response = ratingController.generateCode(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertTrue(result.get("code").toString().startsWith("PRYM-"),
                "Rating code must start with 'PRYM-'");
    }

    // ─── submitRating ─────────────────────────────────────────────────

    // Test 5: submitRating_Success — buyer submits valid code and score 4 → 200 with success message
    @Test
    void submitRating_Success_Returns200() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-ABC123", "score", 4);
        when(ratingService.submitRating(2L, "PRYM-ABC123", 4))
                .thenReturn(Map.of("message", "Rating submitted successfully!"));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Rating submitted successfully!", result.get("message"));
    }

    // Test 6: submitRating_Forbidden — userId in body doesn't match logged-in user → 403
    @Test
    void submitRating_Forbidden_Returns403() {
        loginAs(buyerUser); // logged in as buyer (id=2)
        Map<String, Object> body = Map.of("userId", 99, "code", "PRYM-ABC123", "score", 3);

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", result.get("error"));
        verify(ratingService, never()).submitRating(any(), any(), anyInt());
    }

    // Test 7: submitRating_ScoreTooHigh — service throws "Score must be between 1 and 5" → 400
    @Test
    void submitRating_ScoreTooHigh_Returns400() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-ABC123", "score", 6);
        when(ratingService.submitRating(2L, "PRYM-ABC123", 6))
                .thenThrow(new RuntimeException("Score must be between 1 and 5."));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Score must be between 1 and 5.", result.get("error"));
    }

    // Test 8: submitRating_ScoreTooLow — score 0 → 400
    @Test
    void submitRating_ScoreTooLow_Returns400() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-ABC123", "score", 0);
        when(ratingService.submitRating(2L, "PRYM-ABC123", 0))
                .thenThrow(new RuntimeException("Score must be between 1 and 5."));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertTrue(result.get("error").toString().contains("Score must be between 1 and 5"));
    }

    // Test 9: submitRating_UsedCode — code already used → 400
    @Test
    void submitRating_UsedCode_Returns400() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-USED00", "score", 3);
        when(ratingService.submitRating(2L, "PRYM-USED00", 3))
                .thenThrow(new RuntimeException("Invalid or already-used code."));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Invalid or already-used code.", result.get("error"));
    }

    // Test 10: submitRating_DuplicateRating — buyer already rated this seller → 400
    @Test
    void submitRating_DuplicateRating_Returns400() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-DUP111", "score", 5);
        when(ratingService.submitRating(2L, "PRYM-DUP111", 5))
                .thenThrow(new RuntimeException("You have already rated this seller."));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("You have already rated this seller.", result.get("error"));
    }

    // Test 11: submitRating_InvalidCode_NotFound — code doesn't exist → 400
    @Test
    void submitRating_InvalidCode_Returns400() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-BADXXX", "score", 2);
        when(ratingService.submitRating(2L, "PRYM-BADXXX", 2))
                .thenThrow(new RuntimeException("Invalid or already-used code."));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // Test 12: submitRating_ScoreMinBoundary — score 1 is valid → 200
    @Test
    void submitRating_ScoreOne_MinBoundary_Succeeds() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-MIN111", "score", 1);
        when(ratingService.submitRating(2L, "PRYM-MIN111", 1))
                .thenReturn(Map.of("message", "Rating submitted successfully!"));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 13: submitRating_ScoreMaxBoundary — score 5 is valid → 200
    @Test
    void submitRating_ScoreFive_MaxBoundary_Succeeds() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-MAX555", "score", 5);
        when(ratingService.submitRating(2L, "PRYM-MAX555", 5))
                .thenReturn(Map.of("message", "Rating submitted successfully!"));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
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

    // Test 18: submitRating_BuyerNotFound — service throws "Buyer not found" → 400
    @Test
    void submitRating_BuyerNotFound_Returns400() {
        loginAs(buyerUser);
        Map<String, Object> body = Map.of("userId", 2, "code", "PRYM-NOBUYR", "score", 3);
        when(ratingService.submitRating(2L, "PRYM-NOBUYR", 3))
                .thenThrow(new RuntimeException("Buyer not found"));

        ResponseEntity<?> response = ratingController.submitRating(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("Buyer not found", result.get("error"));
    }

    // Test 19: generateCode_BuyerTriesAsWrongUser — buyer logged in but IDs mismatch → 403
    @Test
    void generateCode_LoggedInAsUserButDifferentId_Returns403() {
        loginAs(buyerUser); // logged in as id=2
        Map<String, Object> body = Map.of("userId", 1); // different from logged-in user

        ResponseEntity<?> response = ratingController.generateCode(body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
