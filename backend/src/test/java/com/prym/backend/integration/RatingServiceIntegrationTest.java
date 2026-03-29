package com.prym.backend.integration;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.BuyerService;
import com.prym.backend.service.RatingService;
import com.prym.backend.service.SellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RatingService.
 * All tests run against the real PostgreSQL database and are rolled back after each test
 * thanks to @Transactional, so they leave no dirty data.
 *
 * Coverage:
 *  - Full generate-code → submit-rating flow
 *  - Correct average calculation across multiple ratings
 *  - Duplicate rating rejection
 *  - Score boundary validation (min=1, max=5)
 *  - Already-used code rejection
 *  - Seller not found edge case
 *  - Buyer not found edge case
 *  - getFarmRatings returns correct aggregates and list
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class RatingServiceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private BuyerService buyerService;
    @Autowired private SellerService sellerService;
    @Autowired private RatingService ratingService;

    // ─── Helpers ──────────────────────────────────────────────────────

    /** Registers a buyer, creates their buyer profile, and returns the User. */
    private User registerBuyer(String email, String username) {
        User user = authService.register(
                email, "pass123", User.Role.BUYER,
                username, "Buyer", "Test", "416-555-0000", null, "10001");
        buyerService.createBuyerProfile(user.getId(), "Chuck");
        return user;
    }

    /** Registers a seller, creates their seller profile, and returns the User. */
    private User registerSeller(String email, String username, String shopName) {
        User user = authService.register(
                email, "pass123", User.Role.SELLER,
                username, "Seller", "Test", "416-555-0001", null, "10001");
        sellerService.createSellerProfile(user.getId(), shopName, "", "");
        return user;
    }

    
    
    // ─── getFarmRatings ────────────────────────────────────────────────

    // Test 13: getFarmRatings_NoRatings — seller with no ratings → average=0, total=0, empty list
    @Test
    void getFarmRatings_NewSeller_ZeroRatings() {
        registerSeller("seller_r13@example.com", "seller_r13", "Fresh Farm");

        Map<String, Object> result = ratingService.getFarmRatings("seller_r13");

        assertEquals("Fresh Farm", result.get("shopName"));
        assertEquals(0.0,          (double) result.get("averageRating"), 0.001);
        assertEquals(0,            result.get("totalRatings"));
        assertTrue(((List<?>) result.get("ratings")).isEmpty());
    }

    // Test 14: getFarmRatings_ReturnsRatingDTOsWithScoreAndDate
    @Test
    void getFarmRatings_ReturnsIndividualRatingDTOs() {
        User seller = registerSeller("seller_r14@example.com", "seller_r14", "Farm R14");
        User buyer  = registerBuyer("buyer_r14@example.com",  "buyer_r14");

        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        ratingService.submitRating(buyer.getId(), code, 3);

        Map<String, Object> result = ratingService.getFarmRatings("seller_r14");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratingDTOs = (List<Map<String, Object>>) result.get("ratings");
        assertEquals(1, ratingDTOs.size());
        assertEquals(3, ratingDTOs.get(0).get("score"));
        assertNotNull(ratingDTOs.get(0).get("createdAt"),
                "Rating DTO must include createdAt timestamp");
    }

    // Test 15: getFarmRatings_SellerNotFound — unknown username → throws
    @Test
    void getFarmRatings_SellerNotFound_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.getFarmRatings("no_such_seller_xyz"));
        assertEquals("Seller not found", ex.getMessage());
    }

    // Test 16: getFarmRatings_AverageRemainsAccurateAfterMultipleRatings
    @Test
    void getFarmRatings_AccumulatedRatings_AverageIsAlwaysAccurate() {
        User seller = registerSeller("seller_r16@example.com", "seller_r16", "Precise Farm");
        User b1 = registerBuyer("buyer_r16a@example.com", "buyer_r16a");
        User b2 = registerBuyer("buyer_r16b@example.com", "buyer_r16b");
        User b3 = registerBuyer("buyer_r16c@example.com", "buyer_r16c");
        User b4 = registerBuyer("buyer_r16d@example.com", "buyer_r16d");

        int[] scores = {1, 2, 4, 5};
        User[] buyers = {b1, b2, b3, b4};
        for (int i = 0; i < scores.length; i++) {
            String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();
            ratingService.submitRating(buyers[i].getId(), code, scores[i]);
        }

        Map<String, Object> result = ratingService.getFarmRatings("seller_r16");
        // (1+2+4+5)/4 = 12/4 = 3.0
        assertEquals(3.0, (double) result.get("averageRating"), 0.001);
        assertEquals(4,   result.get("totalRatings"));
    }

    
}