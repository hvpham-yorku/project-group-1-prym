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

    // ─── Core Flow ────────────────────────────────────────────────────

    // Test 1: generateRatingCode_PersistsCode — code is saved and starts with "PRYM-"
    @Test
    void generateRatingCode_PersistsCodeWithCorrectFormat() {
        User seller = registerSeller("seller_r1@example.com", "seller_r1", "Farm R1");

        Map<String, Object> result = ratingService.generateRatingCode(seller.getId());

        assertNotNull(result.get("code"), "Code must not be null");
        assertTrue(result.get("code").toString().startsWith("PRYM-"),
                "Code must start with 'PRYM-'");
        assertEquals(11, result.get("code").toString().length(),
                "Code must be 11 chars: 'PRYM-' (5) + 6 random chars");
    }

    // Test 2: submitRating_FullFlow — seller generates code, buyer submits rating → success message
    @Test
    void submitRating_FullFlow_SuccessMessage() {
        User seller = registerSeller("seller_r2@example.com", "seller_r2", "Farm R2");
        User buyer  = registerBuyer("buyer_r2@example.com",  "buyer_r2");

        Map<String, Object> codeResult = ratingService.generateRatingCode(seller.getId());
        String code = codeResult.get("code").toString();

        Map<String, Object> ratingResult = ratingService.submitRating(buyer.getId(), code, 4);

        assertEquals("Rating submitted successfully!", ratingResult.get("message"));
    }

    // Test 3: submitRating_UpdatesSellerAverageAndTotal — single rating → averageRating = score
    @Test
    void submitRating_FirstRating_AverageEqualsScore() {
        User seller = registerSeller("seller_r3@example.com", "seller_r3", "Farm R3");
        User buyer  = registerBuyer("buyer_r3@example.com",  "buyer_r3");

        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        ratingService.submitRating(buyer.getId(), code, 5);

        Map<String, Object> ratings = ratingService.getFarmRatings("seller_r3");
        assertEquals(5.0, (double) ratings.get("averageRating"), 0.001);
        assertEquals(1,   ratings.get("totalRatings"));
    }

    // Test 4: submitRating_MultipleRatings_AverageCalculatedCorrectly
    @Test
    void submitRating_TwoRatings_AverageIsCorrect() {
        User seller  = registerSeller("seller_r4@example.com",  "seller_r4", "Farm R4");
        User buyer1  = registerBuyer("buyer_r4a@example.com", "buyer_r4a");
        User buyer2  = registerBuyer("buyer_r4b@example.com", "buyer_r4b");

        String code1 = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        String code2 = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        ratingService.submitRating(buyer1.getId(), code1, 3);
        ratingService.submitRating(buyer2.getId(), code2, 5);

        Map<String, Object> ratings = ratingService.getFarmRatings("seller_r4");
        // (3 + 5) / 2 = 4.0
        assertEquals(4.0, (double) ratings.get("averageRating"), 0.001);
        assertEquals(2,   ratings.get("totalRatings"));
    }

    // Test 5: submitRating_ThreeRatings_AverageCalculatedCorrectly
    @Test
    void submitRating_ThreeRatings_WeightedAverageIsCorrect() {
        User seller = registerSeller("seller_r5@example.com", "seller_r5", "Farm R5");
        User b1 = registerBuyer("buyer_r5a@example.com", "buyer_r5a");
        User b2 = registerBuyer("buyer_r5b@example.com", "buyer_r5b");
        User b3 = registerBuyer("buyer_r5c@example.com", "buyer_r5c");

        String c1 = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        String c2 = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        String c3 = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        ratingService.submitRating(b1.getId(), c1, 2);
        ratingService.submitRating(b2.getId(), c2, 4);
        ratingService.submitRating(b3.getId(), c3, 3);

        Map<String, Object> ratings = ratingService.getFarmRatings("seller_r5");
        // (2 + 4 + 3) / 3 = 3.0
        assertEquals(3.0, (double) ratings.get("averageRating"), 0.001);
        assertEquals(3,   ratings.get("totalRatings"));
    }

    // ─── Validation ───────────────────────────────────────────────────

    // Test 6: submitRating_ScoreTooHigh_Throws — score 6 is invalid
    @Test
    void submitRating_ScoreTooHigh_ThrowsException() {
        User seller = registerSeller("seller_r6@example.com", "seller_r6", "Farm R6");
        User buyer  = registerBuyer("buyer_r6@example.com",  "buyer_r6");
        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(buyer.getId(), code, 6));
        assertEquals("Score must be between 1 and 5.", ex.getMessage());
    }

    // Test 7: submitRating_ScoreTooLow_Throws — score 0 is invalid
    @Test
    void submitRating_ScoreTooLow_ThrowsException() {
        User seller = registerSeller("seller_r7@example.com", "seller_r7", "Farm R7");
        User buyer  = registerBuyer("buyer_r7@example.com",  "buyer_r7");
        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(buyer.getId(), code, 0));
        assertEquals("Score must be between 1 and 5.", ex.getMessage());
    }

    // Test 8: submitRating_ScoreMinBoundary_Succeeds — score 1 is valid
    @Test
    void submitRating_ScoreOne_MinBoundary_Succeeds() {
        User seller = registerSeller("seller_r8@example.com", "seller_r8", "Farm R8");
        User buyer  = registerBuyer("buyer_r8@example.com",  "buyer_r8");
        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        assertDoesNotThrow(() -> ratingService.submitRating(buyer.getId(), code, 1));

        Map<String, Object> ratings = ratingService.getFarmRatings("seller_r8");
        assertEquals(1.0, (double) ratings.get("averageRating"), 0.001);
    }

    // Test 9: submitRating_ScoreMaxBoundary_Succeeds — score 5 is valid
    @Test
    void submitRating_ScoreFive_MaxBoundary_Succeeds() {
        User seller = registerSeller("seller_r9@example.com", "seller_r9", "Farm R9");
        User buyer  = registerBuyer("buyer_r9@example.com",  "buyer_r9");
        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        assertDoesNotThrow(() -> ratingService.submitRating(buyer.getId(), code, 5));

        Map<String, Object> ratings = ratingService.getFarmRatings("seller_r9");
        assertEquals(5.0, (double) ratings.get("averageRating"), 0.001);
    }

    // Test 10: submitRating_UsedCode_Throws — code already consumed → throws
    @Test
    void submitRating_UsedCode_ThrowsException() {
        User seller = registerSeller("seller_r10@example.com", "seller_r10", "Farm R10");
        User buyer1 = registerBuyer("buyer_r10a@example.com", "buyer_r10a");
        User buyer2 = registerBuyer("buyer_r10b@example.com", "buyer_r10b");

        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        ratingService.submitRating(buyer1.getId(), code, 4); // uses the code

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(buyer2.getId(), code, 3)); // reuse fails
        assertEquals("Invalid or already-used code.", ex.getMessage());
    }

    // Test 11: submitRating_DuplicateRating_Throws — buyer already rated this seller
    @Test
    void submitRating_SameBuyerRatesSameSeller_Throws() {
        User seller = registerSeller("seller_r11@example.com", "seller_r11", "Farm R11");
        User buyer  = registerBuyer("buyer_r11@example.com",  "buyer_r11");

        String code1 = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        String code2 = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        ratingService.submitRating(buyer.getId(), code1, 4); // first rating — ok

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(buyer.getId(), code2, 5)); // second — blocked
        assertEquals("You have already rated this seller.", ex.getMessage());
    }

    // Test 12: submitRating_InvalidCode_Throws — made-up code → throws
    @Test
    void submitRating_NonExistentCode_ThrowsException() {
        User buyer = registerBuyer("buyer_r12@example.com", "buyer_r12");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(buyer.getId(), "PRYM-XXXXXX", 3));
        assertEquals("Invalid or already-used code.", ex.getMessage());
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

    // ─── generateRatingCode — seller-only ─────────────────────────────

    // Test 17: generateRatingCode_SellerNotFound_Throws
    @Test
    void generateRatingCode_NonExistentSeller_Throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.generateRatingCode(999999L));
        assertEquals("Seller not found", ex.getMessage());
    }

    // Test 18: generateRatingCode_TwoCallsProduceDifferentCodes — codes are unique
    @Test
    void generateRatingCode_CalledTwice_ProducesDifferentCodes() {
        User seller = registerSeller("seller_r18@example.com", "seller_r18", "Farm R18");

        String code1 = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        String code2 = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        assertNotEquals(code1, code2, "Two generated codes should not be equal");
    }

    // Test 19: submitRating_BuyerNotFound_Throws
    @Test
    void submitRating_BuyerNotFound_Throws() {
        User seller = registerSeller("seller_r19@example.com", "seller_r19", "Farm R19");
        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(999999L, code, 3));
        assertEquals("Buyer not found", ex.getMessage());
    }

    // Test 20: submitRating_CodeIsNormalizedToUppercase — lowercase code is accepted
    @Test
    void submitRating_LowercaseCode_NormalizedAndAccepted() {
        User seller = registerSeller("seller_r20@example.com", "seller_r20", "Farm R20");
        User buyer  = registerBuyer("buyer_r20@example.com",  "buyer_r20");

        String code = ratingService.generateRatingCode(seller.getId()).get("code").toString();
        String lowerCode = code.toLowerCase();

        // Service should normalize to uppercase before lookup
        assertDoesNotThrow(() -> ratingService.submitRating(buyer.getId(), lowerCode, 4),
                "Lowercase code should be accepted after normalization");
    }
}
