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
     * User Story: As a seller, I want to update my farm details
     * so buyers always see accurate information about my farm.
     * Acceptance: Updated fields are persisted and returned on next retrieval.
     */
    @Test
    void seller_UpdatesFarmProfile() {
        User user = registerSeller("updatefarm@example.com", "updatefarm", "Old Name Farm");

        sellerService.updateSellerProfile(user.getId(), "Updated Farm", null, "789 New Road", "Premium cuts");
        Seller updated = sellerService.getSellerProfile(user.getId());

        assertEquals("Updated Farm", updated.getShopName());
        assertEquals("789 New Road", updated.getShopAddress());
        assertEquals("Premium cuts", updated.getDescription());
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

    // ─── Journey 2: Seller manages certifications ─────────────────────────────

    /**
     * User Story: As a seller, I want to add certifications to my farm profile
     * so buyers with specific dietary requirements can find me.
     * Acceptance: Added certifications are saved and retrievable.
     */
    @Test
    void seller_AddsCertificationsToProfile() {
        User user = registerSeller("certfarm@example.com", "certfarm", "Cert Farm");

        certificationService.addCertification(user.getId(), "HALAL", "Halal Authority", null);
        certificationService.addCertification(user.getId(), "ORGANIC", "USDA", LocalDate.of(2027, 12, 31));

        List<Certification> certs = certificationService.getCertificationsBySeller(user.getId());

        assertEquals(2, certs.size());
        assertTrue(certs.stream().anyMatch(c -> "HALAL".equals(c.getName().name())));
        assertTrue(certs.stream().anyMatch(c -> "ORGANIC".equals(c.getName().name())));
    }

    /**
     * User Story: As a seller, I want to replace all my certifications at once
     * so I can keep them up to date without managing them individually.
     * Acceptance: After a full replace, only the new certifications exist.
     */
    @Test
    void seller_ReplacesCertificationsInBulk() {
        User user = registerSeller("replcertfarm@example.com", "replcertfarm", "Replace Cert Farm");
        certificationService.addCertification(user.getId(), "HALAL", "Authority", null);

        certificationService.setCertifications(user.getId(), List.of("KOSHER", "NON_GMO"));
        List<Certification> certs = certificationService.getCertificationsBySeller(user.getId());

        assertEquals(2, certs.size());
        assertTrue(certs.stream().anyMatch(c -> "KOSHER".equals(c.getName().name())));
        assertTrue(certs.stream().anyMatch(c -> "NON_GMO".equals(c.getName().name())));
        assertFalse(certs.stream().anyMatch(c -> "HALAL".equals(c.getName().name())),
                "Old HALAL cert must be gone after full replace");
    }

    /**
     * User Story: As a seller, I want to delete a specific certification
     * so I can remove ones that are no longer valid.
     * Acceptance: After deletion, the certification no longer appears in the list.
     */
    @Test
    void seller_DeletesCertification() {
        User user = registerSeller("delcertfarm@example.com", "delcertfarm", "Del Cert Farm");
        Certification cert = certificationService.addCertification(user.getId(), "GRASS_FED", "Certifier", null);

        certificationService.deleteCertification(user.getId(), cert.getId());
        List<Certification> remaining = certificationService.getCertificationsBySeller(user.getId());

        assertTrue(remaining.isEmpty(), "Certification must be deleted");
    }

    // ─── Journey 3: Seller creates cow types and lists cows ───────────────────

    /**
     * User Story: As a seller, I want to create cow types with breed and pricing
     * so buyers know what I offer and at what price.
     * Acceptance: Cow type is saved with the correct breed and price.
     */
    @Test
    void seller_CreatesCowType() {
        User user = registerSeller("cowtypefarm@example.com", "cowtypefarm", "Cow Type Farm");

        CowType cowType = cowTypeService.createCowType(user.getId(), "ANGUS", "Prime Angus beef", 14.99, 3);

        assertNotNull(cowType.getId());
        assertEquals("ANGUS", cowType.getBreed().name());
        assertEquals(14.99, cowType.getPricePerPound(), 0.001);
        assertEquals(3, cowType.getAvailableCount());
    }

    /**
     * User Story: As a seller, I want to list an individual cow for sale
     * so buyers can see what is currently available.
     * Acceptance: Cow is created with the correct details and auto-generates 22 cuts.
     */
    @Test
    void seller_ListsACow() {
        User user = registerSeller("cowfarm@example.com", "cowfarm", "Cow Farm");
        CowType cowType = cowTypeService.createCowType(user.getId(), "WAGYU", "Premium Wagyu", 29.99, 1);

        Cow cow = cowService.createCow(cowType.getId(), "Bessie", 900.0, LocalDate.of(2026, 8, 1));

        assertNotNull(cow.getId());
        assertEquals("Bessie", cow.getName());
        assertEquals(Cow.CowStatus.OPEN, cow.getStatus());

        List<CowCut> cuts = cowService.getAllCuts(cow.getId());
        assertEquals(22, cuts.size(), "Every cow must auto-generate exactly 22 cuts");
    }

    /**
     * User Story: As a seller, I want buyers to be able to view the available cuts on my cow
     * so they can plan their purchase.
     * Acceptance: All 22 cuts start as AVAILABLE immediately after the cow is listed.
     */
    @Test
    void seller_AllCutsStartAsAvailable() {
        User user = registerSeller("availfarm@example.com", "availfarm", "Avail Farm");
        CowType cowType = cowTypeService.createCowType(user.getId(), "HERITAGE", "Heritage breed", 12.00, 2);
        Cow cow = cowService.createCow(cowType.getId(), "Daisy", 750.0, LocalDate.of(2026, 9, 1));

        List<CowCut> available = cowService.getAvailableCuts(cow.getId());

        assertEquals(22, available.size(), "All 22 cuts must be AVAILABLE when the cow is first listed");
    }

    /**
     * User Story: As a seller, I want to update my cow types
     * so I can adjust pricing or availability as things change.
     * Acceptance: Updated price and count are persisted correctly.
     */
    @Test
    void seller_UpdatesCowType() {
        User user = registerSeller("updatecowfarm@example.com", "updatecowfarm", "Update Cow Farm");
        CowType cowType = cowTypeService.createCowType(user.getId(), "CONVENTIONAL", "Standard beef", 8.00, 5);

        CowType updated = cowTypeService.updateCowType(cowType.getId(), null, "Updated description", 9.50, 3);

        assertEquals(9.50, updated.getPricePerPound(), 0.001);
        assertEquals(3, updated.getAvailableCount());
    }

    // ─── Journey 4: Seller generates a rating code after a sale ──────────────

    /**
     * User Story: As a seller, I want to generate a one-time rating code after a transaction
     * so the buyer can submit a rating for my farm.
     * Acceptance: A unique PRYM-XXXXXX code is generated and returned.
     */
    @Test
    void seller_GeneratesRatingCode() {
        User user = registerSeller("ratingcodefarm@example.com", "ratingcodefarm", "Rating Code Farm");

        Map<String, Object> result = ratingService.generateRatingCode(user.getId());

        assertNotNull(result.get("code"), "A rating code must be returned");
        assertTrue(result.get("code").toString().startsWith("PRYM-"),
                "Rating code must start with 'PRYM-'");
        assertEquals(11, result.get("code").toString().length(),
                "Rating code must be exactly 11 characters (PRYM- + 6)");
    }

    /**
     * User Story: As a seller, I want each rating code to be unique
     * so codes from different transactions cannot be confused.
     * Acceptance: Two generated codes are different.
     */
    @Test
    void seller_EachGeneratedCodeIsUnique() {
        User user = registerSeller("uniquecodefarm@example.com", "uniquecodefarm", "Unique Code Farm");

        String code1 = (String) ratingService.generateRatingCode(user.getId()).get("code");
        String code2 = (String) ratingService.generateRatingCode(user.getId()).get("code");

        assertNotEquals(code1, code2, "Two generated codes must be unique");
    }

    /**
     * User Story: As a seller, I want my average rating to update each time a buyer rates me
     * so the displayed score always reflects current buyer feedback.
     * Acceptance: After two ratings, the displayed average is correct.
     */
    @Test
    void seller_AverageRatingUpdatesAfterEachSubmission() {
        User sellerUser = registerSeller("avgratingfarm@example.com", "avgratingfarm", "Avg Rating Farm");
        User buyer1 = registerBuyer("avgbuyer1@example.com", "avgbuyer1");
        User buyer2 = registerBuyer("avgbuyer2@example.com", "avgbuyer2");

        String code1 = (String) ratingService.generateRatingCode(sellerUser.getId()).get("code");
        String code2 = (String) ratingService.generateRatingCode(sellerUser.getId()).get("code");

        ratingService.submitRating(buyer1.getId(), code1, 5);
        ratingService.submitRating(buyer2.getId(), code2, 3);

        Map<String, Object> ratings = ratingService.getFarmRatings("avgratingfarm");
        assertEquals(4.0, (double) ratings.get("averageRating"), 0.001,
                "Average of 5 and 3 must be 4.0");
        assertEquals(2, ratings.get("totalRatings"));
    }
}
