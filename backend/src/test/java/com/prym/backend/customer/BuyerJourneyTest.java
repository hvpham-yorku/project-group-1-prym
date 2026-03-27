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

    // ─── Journey 1: Buyer registers and views their profile ───────────────────

    /**
     * User Story: As a buyer, I want to register an account so I can use the platform.
     * Acceptance: After registration, my profile exists and reflects the data I entered.
     */
    @Test
    void buyer_RegistersAndProfileIsAccessible() {
        User user = authService.register(
                "newbuyer@example.com", "securepass", User.Role.BUYER,
                "newbuyer", "Alice", "Smith", "647-555-0001", null, "M5V2T6");

        assertNotNull(user.getId(), "User must be assigned a database ID after registration");
        assertEquals("newbuyer@example.com", user.getEmail());
        assertEquals("newbuyer", user.getUsername());
        assertEquals(User.Role.BUYER, user.getRole());
    }

    /**
     * User Story: As a buyer, I want to set up my profile with preferred cuts
     * so the system knows what I am looking for.
     * Acceptance: Profile is saved and retrievable with the correct preferred cuts.
     */
    @Test
    void buyer_SetsUpProfileWithPreferredCuts() {
        User user = registerBuyer("cutbuyer@example.com", "cutbuyer");

        Buyer profile = buyerService.getBuyerProfile(user.getId());

        assertNotNull(profile);
        assertEquals("Chuck, Rib", profile.getPreferredCuts());
    }

    /**
     * User Story: As a buyer, I want to update my preferred cuts after signup
     * so I can change my preferences over time.
     * Acceptance: Updated cuts are persisted and returned correctly.
     */
    @Test
    void buyer_UpdatesPreferredCuts() {
        User user = registerBuyer("updatebuyer@example.com", "updatebuyer");

        buyerService.updateBuyerProfile(user.getId(), "Sirloin, Brisket x2", null);
        Buyer updated = buyerService.getBuyerProfile(user.getId());

        assertEquals("Sirloin, Brisket x2", updated.getPreferredCuts());
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
     * User Story: As a buyer, I want to save farms I'm interested in
     * so I can quickly find them later.
     * Acceptance: After saving, the farm appears in my saved farms list.
     */
    @Test
    void buyer_SavesAndRetrievesFarm() {
        User buyer = registerBuyer("savebuyer@example.com", "savebuyer");
        User sellerUser = registerSeller("savefarm@example.com", "savefarm", "Save Farm");
        Seller farm = sellerService.getSellerProfile(sellerUser.getId());

        buyerService.saveFarm(buyer.getId(), farm);
        List<Seller> saved = buyerService.getSavedFarms(buyer.getId());

        assertEquals(1, saved.size());
        assertEquals("Save Farm", saved.get(0).getShopName());
    }

    /**
     * User Story: As a buyer, I want to remove a farm from my saved list
     * so I can keep my list relevant.
     * Acceptance: After removal, the farm no longer appears in saved farms.
     */
    @Test
    void buyer_RemovesSavedFarm() {
        User buyer = registerBuyer("removebuyer@example.com", "removebuyer");
        User sellerUser = registerSeller("removefarm@example.com", "removefarm", "Remove Farm");
        Seller farm = sellerService.getSellerProfile(sellerUser.getId());

        buyerService.saveFarm(buyer.getId(), farm);
        assertEquals(1, buyerService.getSavedFarms(buyer.getId()).size());

        buyerService.removeSavedFarm(buyer.getId(), farm.getId());
        assertEquals(0, buyerService.getSavedFarms(buyer.getId()).size(),
                "Farm must be removed from saved list");
    }

    // ─── Journey 3: Buyer creates a group and invites others ──────────────────

    /**
     * User Story: As a buyer, I want to create a buying group with certification filters
     * so I can find like-minded buyers who share my dietary requirements.
     * Acceptance: Group is created with correct name, certifications, and creator is auto-joined.
     */
    @Test
    void buyer_CreatesGroupWithCertifications() {
        User buyer = registerBuyer("groupcreator@example.com", "groupcreator");

        Map<String, Object> result = groupService.createGroup(buyer.getId(), "Halal Crew", "HALAL,ORGANIC");

        assertNotNull(result.get("groupId"), "Group must be assigned an ID");
        assertEquals("Halal Crew", result.get("groupName"));
        assertEquals(List.of("HALAL", "ORGANIC"), result.get("certifications"));
        assertEquals(1, result.get("memberCount"), "Creator must be auto-added as first member");
        assertTrue((Boolean) result.get("alreadyJoined"));
    }

    /**
     * User Story: As a buyer, I want to share my group's invite code
     * so others can find and join my group.
     * Acceptance: The group can be looked up by its invite code.
     */
    @Test
    void buyer_GroupIsReachableByInviteCode() {
        User creator = registerBuyer("invitecreator@example.com", "invitecreator");
        User joiner = registerBuyer("invitejoiner@example.com", "invitejoiner");

        Map<String, Object> group = groupService.createGroup(creator.getId(), "Code Group", "KOSHER");
        String inviteCode = (String) group.get("inviteCode");
        assertNotNull(inviteCode, "Group must have an invite code");

        Map<String, Object> found = groupService.getGroupByCode(joiner.getId(), inviteCode);
        assertEquals("Code Group", found.get("groupName"));
    }

    /**
     * User Story: As a buyer, I want to regenerate my group's invite code
     * so I can invalidate the old one if needed.
     * Acceptance: The new code is different from the original.
     */
    @Test
    void buyer_RegeneratesInviteCode() {
        User creator = registerBuyer("regenbuyer@example.com", "regenbuyer");
        Map<String, Object> group = groupService.createGroup(creator.getId(), "Regen Group", "");
        Long groupId = ((Number) group.get("groupId")).longValue();
        String originalCode = (String) group.get("inviteCode");

        Map<String, Object> result = groupService.regenerateInviteCode(creator.getId(), groupId);
        String newCode = (String) result.get("inviteCode");

        assertNotNull(newCode);
        assertNotEquals(originalCode, newCode, "Regenerated code must differ from the original");
    }

    // ─── Journey 4: Buyer joins a group and selects cuts ─────────────────────

    /**
     * User Story: As a buyer, I want to join a group
     * so I can participate in a shared cow purchase.
     * Acceptance: After joining, the group's member count increases and I appear as a member.
     */
    @Test
    void buyer_JoinsGroup() {
        User creator = registerBuyer("joincreator@example.com", "joincreator");
        User joiner = registerBuyer("joinjoiner@example.com", "joinjoiner");

        Map<String, Object> group = groupService.createGroup(creator.getId(), "Join Test", "");
        Long groupId = ((Number) group.get("groupId")).longValue();

        groupService.joinGroup(joiner.getId(), groupId);

        Map<String, Object> details = groupService.getGroup(creator.getId(), groupId);
        assertEquals(2, details.get("memberCount"), "Group must have 2 members after joiner joins");
    }

    /**
     * User Story: As a buyer in a group, I want to select my preferred cuts
     * so the group can plan the full cow purchase together.
     * Acceptance: My cut selection is saved and visible in my group membership.
     */
    @Test
    void buyer_SelectsCutsInGroup() {
        User buyer = registerBuyer("cutselector@example.com", "cutselector");
        Map<String, Object> group = groupService.createGroup(buyer.getId(), "Cut Group", "");
        Long groupId = ((Number) group.get("groupId")).longValue();

        Map<String, Object> result = groupService.saveCuts(buyer.getId(), groupId, "Chuck, Rib x2");

        assertNotNull(result);
        assertTrue(result.containsKey("members"), "Result must include members");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) result.get("members");
        boolean found = members.stream()
                .anyMatch(m -> "Chuck, Rib x2".equals(m.get("claimedCuts")));
        assertTrue(found, "Buyer's claimed cuts must be saved in the group");
    }

    /**
     * User Story: As a buyer, I want to leave a group
     * so I can exit if my plans change.
     * Acceptance: After leaving, the group's member count decreases.
     */
    @Test
    void buyer_LeavesGroup() {
        User creator = registerBuyer("leavecreator@example.com", "leavecreator");
        User leaver = registerBuyer("leavejoiner@example.com", "leavejoiner");

        Map<String, Object> group = groupService.createGroup(creator.getId(), "Leave Test", "");
        Long groupId = ((Number) group.get("groupId")).longValue();
        groupService.joinGroup(leaver.getId(), groupId);

        assertEquals(2, ((Map<?, ?>) groupService.getGroup(creator.getId(), groupId)).get("memberCount"));

        groupService.leaveGroup(leaver.getId(), groupId);

        Map<String, Object> details = groupService.getGroup(creator.getId(), groupId);
        assertEquals(1, details.get("memberCount"), "Member count must drop to 1 after leaver exits");
    }

    // ─── Journey 5: Buyer submits a farm rating ───────────────────────────────

    /**
     * User Story: As a buyer, I want to rate a farm after a purchase
     * so other buyers can make informed decisions.
     * Acceptance: Rating is submitted successfully and the farm's average updates.
     */
    @Test
    void buyer_SubmitsRatingForFarm() {
        User sellerUser = registerSeller("ratingseller@example.com", "ratingseller", "Rated Farm");
        User buyerUser = registerBuyer("ratingbuyer@example.com", "ratingbuyer");

        Map<String, Object> codeResult = ratingService.generateRatingCode(sellerUser.getId());
        String code = (String) codeResult.get("code");

        Map<String, Object> result = ratingService.submitRating(buyerUser.getId(), code, 5);
        assertEquals("Rating submitted successfully!", result.get("message"));

        Map<String, Object> ratings = ratingService.getFarmRatings("ratingseller");
        assertEquals(5.0, (double) ratings.get("averageRating"), 0.001);
        assertEquals(1, ratings.get("totalRatings"));
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
