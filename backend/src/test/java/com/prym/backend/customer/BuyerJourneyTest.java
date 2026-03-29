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

}