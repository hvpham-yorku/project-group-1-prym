package com.prym.backend.customer;

import com.prym.backend.model.*;
import com.prym.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Customer acceptance tests for Seller user journeys.
 *
 * These tests simulate complete end-to-end flows from a seller's perspective:
 *   - Register an account and set up a farm profile
 *   - Add and manage certifications
 *   - Create cow types and list individual cows
 *   - Generate rating codes after a transaction
 *   - Update farm profile details
 *
 * All tests run against the real PostgreSQL database and are rolled back after each test.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class SellerJourneyTest {

    @Autowired private AuthService authService;
    @Autowired private SellerService sellerService;
    @Autowired private CertificationService certificationService;
    @Autowired private CowTypeService cowTypeService;
    @Autowired private CowService cowService;
    @Autowired private RatingService ratingService;
    @Autowired private BuyerService buyerService;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User registerSeller(String email, String username, String shopName) {
        User user = authService.register(
                email, "password123", User.Role.SELLER,
                username, "Farm", "Owner", "416-555-3000", null, "10001");
        sellerService.createSellerProfile(user.getId(), shopName, "456 Farm Lane", "Fresh from the farm");
        return user;
    }

    private User registerBuyer(String email, String username) {
        User user = authService.register(
                email, "password123", User.Role.BUYER,
                username, "Jane", "Buyer", "416-555-4000", null, "10001");
        buyerService.createBuyerProfile(user.getId(), "Chuck");
        return user;
    }
  // ─── Journey 1: Seller registers and sets up their farm ───────────────────

    /**
     * User Story: As a seller, I want to register and set up my farm profile
     * so buyers can discover my farm.
     * Acceptance: After registration, the farm profile is accessible with the correct details.
     */
    @Test
    void seller_RegistersAndFarmProfileIsAccessible() {
        User user = registerSeller("newfarm@example.com", "newfarm", "New Farm");

        Seller profile = sellerService.getSellerProfile(user.getId());

        assertNotNull(profile.getId());
        assertEquals("New Farm", profile.getShopName());
        assertEquals("456 Farm Lane", profile.getShopAddress());
        assertEquals("Fresh from the farm", profile.getDescription());
    }

  
    /**
     * User Story: As a seller, I want my farm to appear in the public listings
     * so buyers can find me when browsing.
     * Acceptance: Farm appears in the list returned by getAllFarms.
     */
    @Test
    void seller_FarmAppearsInPublicListings() {
        registerSeller("listedfarm@example.com", "listedfarm", "Listed Farm");

        List<Seller> allFarms = sellerService.getAllFarms();

        assertTrue(allFarms.stream().anyMatch(s -> "Listed Farm".equals(s.getShopName())),
                "Farm must appear in the public listings");
    }

   
}
