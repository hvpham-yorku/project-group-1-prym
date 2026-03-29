package com.prym.backend.customer;

import com.prym.backend.model.*;
import com.prym.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Customer acceptance tests for Buyer user journeys.
 *
 * These tests simulate complete end-to-end flows from a buyer's perspective:
 *   - Register an account
 *   - Set up a buyer profile
 *   - Browse and save farms
 *   - Create and join buying groups
 *   - Select cuts within a group
 *   - Submit a rating using a seller-generated code
 *
 * All tests run against the real PostgreSQL database and are rolled back after each test.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class BuyerJourneyTest {

    @Autowired private AuthService authService;
    @Autowired private BuyerService buyerService;
    @Autowired private SellerService sellerService;
    @Autowired private GroupService groupService;
    @Autowired private RatingService ratingService;
    @Autowired private CertificationService certificationService;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User registerBuyer(String email, String username) {
        User user = authService.register(
                email, "password123", User.Role.BUYER,
                username, "Jane", "Buyer", "416-555-1000", null, "10001");
        buyerService.createBuyerProfile(user.getId(), "Chuck, Rib");
        return user;
    }

    private User registerSeller(String email, String username, String shopName) {
        User user = authService.register(
                email, "password123", User.Role.SELLER,
                username, "John", "Seller", "416-555-2000", null, "10001");
        sellerService.createSellerProfile(user.getId(), shopName, "123 Farm Rd", "Quality beef farm");
        return user;
    }

   

    // ─── Journey 2: Buyer browses and saves farms ─────────────────────────────

    /**
     * User Story: As a buyer, I want to browse all available farms
     * so I can find one that suits my needs.
     * Acceptance: The farm list includes the farm I just created.
     */
    @Test
    void buyer_BrowsesFarmListings() {
        registerSeller("browsefarm@example.com", "browsefarm", "Browse Farm");

        List<Seller> farms = sellerService.getAllFarms();

        assertTrue(farms.stream().anyMatch(s -> "Browse Farm".equals(s.getShopName())),
                "Farm listings must include the newly created farm");
    }
    
 /**
     * User Story: As a buyer, I want to view a farm's ratings before committing
     * so I can trust the seller's quality.
     * Acceptance: Farm ratings page shows shop name, average, total count, and individual scores.
     */
    @Test
    void buyer_ViewsFarmRatings() {
        User sellerUser = registerSeller("viewrateseller@example.com", "viewrateseller", "Reviewed Farm");
        User buyer1 = registerBuyer("reviewer1@example.com", "reviewer1");
        User buyer2 = registerBuyer("reviewer2@example.com", "reviewer2");

        String code1 = (String) ratingService.generateRatingCode(sellerUser.getId()).get("code");
        String code2 = (String) ratingService.generateRatingCode(sellerUser.getId()).get("code");

        ratingService.submitRating(buyer1.getId(), code1, 4);
        ratingService.submitRating(buyer2.getId(), code2, 2);

        Map<String, Object> ratings = ratingService.getFarmRatings("viewrateseller");

        assertEquals("Reviewed Farm", ratings.get("shopName"));
        assertEquals(3.0, (double) ratings.get("averageRating"), 0.001, "Average of 4 and 2 must be 3.0");
        assertEquals(2, ratings.get("totalRatings"));
        @SuppressWarnings("unchecked")
        List<?> ratingList = (List<?>) ratings.get("ratings");
        assertEquals(2, ratingList.size());
    }
}