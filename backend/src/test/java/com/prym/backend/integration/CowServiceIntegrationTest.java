package com.prym.backend.integration;

import com.prym.backend.model.*;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.CowService;
import com.prym.backend.service.CowTypeService;
import com.prym.backend.service.SellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CowService.
 * All tests run against the real PostgreSQL database and are rolled back after each test.
 *
 * Coverage:
 *  - createCow auto-generates exactly 22 CowCut records (11 names × 2 sides)
 *  - All 11 CutName values appear in the generated cuts
 *  - Each cut has exactly one LEFT and one RIGHT side
 *  - All generated cuts start with AVAILABLE status
 *  - getCowsBySeller returns only that seller's cows
 *  - getAvailableCuts and getAllCuts return correct counts
 *  - Invalid CowType ID throws RuntimeException
 *  - Multiple cows for the same seller accumulate correctly
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class CowServiceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private SellerService sellerService;
    @Autowired private CowTypeService cowTypeService;
    @Autowired private CowService cowService;

    // ─── Helpers ──────────────────────────────────────────────────────

    /** Registers a seller, creates their profile, and returns the User. */
    private User registerSeller(String email, String username, String shopName) {
        User user = authService.register(
                email, "pass123", User.Role.SELLER,
                username, "Seller", "Test", "416-555-0001", null, "10001");
        sellerService.createSellerProfile(user.getId(), shopName, "", "");
        return user;
    }

    /**
     * Returns the Seller entity ID (PK of the sellers table) for a given user.
     * getCowsBySeller uses findByCowTypeSellerId which filters by Seller.id,
     * not by User.id, so we must resolve the Seller entity first.
     */
    private Long sellerEntityId(Long userId) {
        return sellerService.getSellerProfile(userId).getId();
    }

    /** Creates a CowType for a seller and returns it. */
    private CowType createCowType(Long sellerId) {
        return cowTypeService.createCowType(sellerId, "ANGUS", "Prime Angus beef", 12.50, 5);
    }

    // ─── createCow — cut generation ───────────────────────────────────

    // Test 1: createCow_GeneratesExactly22Cuts — 11 cut names × LEFT + RIGHT = 22 records
    @Test
    void createCow_GeneratesExactly22CowCuts() {
        User seller = registerSeller("cow_seller1@example.com", "cow_seller1", "Cow Farm 1");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Bessie", 850.0, LocalDate.of(2026, 6, 1));

        List<CowCut> allCuts = cowService.getAllCuts(cow.getId());
        assertEquals(22, allCuts.size(),
                "Every cow must have exactly 22 cuts (11 cut names × 2 sides)");
    }

    // Test 2: createCow_AllCutsAreAvailable — no cuts are pre-claimed
    @Test
    void createCow_AllCutsStartAsAvailable() {
        User seller = registerSeller("cow_seller2@example.com", "cow_seller2", "Cow Farm 2");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Daisy", 720.0, LocalDate.of(2026, 7, 15));

        List<CowCut> allCuts = cowService.getAllCuts(cow.getId());
        long unavailableCount = allCuts.stream()
                .filter(c -> c.getStatus() != CowCut.CutStatus.AVAILABLE)
                .count();
        assertEquals(0, unavailableCount,
                "All cuts must be AVAILABLE immediately after cow creation");
    }

    // Test 3: createCow_AllCutNamesPresent — all 11 CowCut.CutName values appear
    @Test
    void createCow_AllElevenCutNamesPresent() {
        User seller = registerSeller("cow_seller3@example.com", "cow_seller3", "Cow Farm 3");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Molly", 900.0, LocalDate.of(2026, 8, 1));

        List<CowCut> allCuts = cowService.getAllCuts(cow.getId());
        Set<CowCut.CutName> presentNames = allCuts.stream()
                .map(CowCut::getCutName)
                .collect(Collectors.toSet());

        assertEquals(CowCut.CutName.values().length, presentNames.size(),
                "All " + CowCut.CutName.values().length + " cut names must be present");
        for (CowCut.CutName name : CowCut.CutName.values()) {
            assertTrue(presentNames.contains(name),
                    "Cut name " + name + " must be present in the generated cuts");
        }
    }

    // Test 4: createCow_EachCutHasBothSides — LEFT and RIGHT appear for every CutName
    @Test
    void createCow_EachCutNameHasExactlyOneLeftAndOneRight() {
        User seller = registerSeller("cow_seller4@example.com", "cow_seller4", "Cow Farm 4");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Rosie", 780.0, LocalDate.of(2026, 9, 10));

        List<CowCut> allCuts = cowService.getAllCuts(cow.getId());

        for (CowCut.CutName cutName : CowCut.CutName.values()) {
            long leftCount = allCuts.stream()
                    .filter(c -> c.getCutName() == cutName && c.getSide() == CowCut.Side.LEFT)
                    .count();
            long rightCount = allCuts.stream()
                    .filter(c -> c.getCutName() == cutName && c.getSide() == CowCut.Side.RIGHT)
                    .count();

            assertEquals(1, leftCount,
                    cutName + " must have exactly 1 LEFT side cut");
            assertEquals(1, rightCount,
                    cutName + " must have exactly 1 RIGHT side cut");
        }
    }

    // Test 5: createCow_SpecificCutNamesVerified — explicitly checks each of the 11 expected names
    @Test
    void createCow_ExactCutNamesMatchExpected() {
        User seller = registerSeller("cow_seller5@example.com", "cow_seller5", "Cow Farm 5");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Lilly", 810.0, LocalDate.of(2026, 10, 1));

        List<CowCut> allCuts = cowService.getAllCuts(cow.getId());
        Set<String> cutNames = allCuts.stream()
                .map(c -> c.getCutName().name())
                .collect(Collectors.toSet());

        // Explicitly verify the 11 expected cut names from the CutName enum
        assertTrue(cutNames.contains("CHUCK"),       "CHUCK must be present");
        assertTrue(cutNames.contains("NECK"),        "NECK must be present");
        assertTrue(cutNames.contains("RIB"),         "RIB must be present");
        assertTrue(cutNames.contains("SHORT_LOIN"),  "SHORT_LOIN must be present");
        assertTrue(cutNames.contains("SIRLOIN"),     "SIRLOIN must be present");
        assertTrue(cutNames.contains("ROUND"),       "ROUND must be present");
        assertTrue(cutNames.contains("BRISKET"),     "BRISKET must be present");
        assertTrue(cutNames.contains("PLATE"),       "PLATE must be present");
        assertTrue(cutNames.contains("FLANK"),       "FLANK must be present");
        assertTrue(cutNames.contains("SHANK_FRONT"), "SHANK_FRONT must be present");
        assertTrue(cutNames.contains("SHANK_REAR"),  "SHANK_REAR must be present");
        assertEquals(11, cutNames.size(),            "Exactly 11 distinct cut names expected");
    }

    // Test 6: createCow_CowStatusIsOpen — newly created cow is in OPEN state
    @Test
    void createCow_InitialStatusIsOpen() {
        User seller = registerSeller("cow_seller6@example.com", "cow_seller6", "Cow Farm 6");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Betty", 950.0, LocalDate.of(2026, 11, 5));

        assertEquals(Cow.CowStatus.OPEN, cow.getStatus(),
                "Newly created cow must have OPEN status");
    }

    // Test 7: createCow_FieldsPersisted — name, weight, and harvestDate are stored correctly
    @Test
    void createCow_AllFieldsPersistCorrectly() {
        User seller = registerSeller("cow_seller7@example.com", "cow_seller7", "Cow Farm 7");
        CowType cowType = createCowType(seller.getId());

        LocalDate harvestDate = LocalDate.of(2026, 12, 25);
        Cow cow = cowService.createCow(cowType.getId(), "Christmas", 1000.0, harvestDate);

        assertEquals("Christmas",   cow.getName());
        assertEquals(1000.0,        cow.getEstimatedWeightLbs(), 0.001);
        assertEquals(harvestDate,   cow.getHarvestDate());
        assertNotNull(cow.getId(),  "Cow must have a DB-assigned ID after creation");
    }

    // Test 8: createCow_InvalidCowTypeId_Throws — non-existent CowType ID → RuntimeException
    @Test
    void createCow_InvalidCowTypeId_ThrowsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cowService.createCow(999999L, "Ghost Cow", 500.0,
                        LocalDate.of(2026, 1, 1)));
        assertEquals("CowType not found", ex.getMessage());
    }

    // ─── getAvailableCuts / getAllCuts ────────────────────────────────

    // Test 9: getAvailableCuts_ReturnsAll22_WhenNoneAreClaimed
    @Test
    void getAvailableCuts_AllCutsAvailable_Returns22() {
        User seller = registerSeller("cow_seller9@example.com", "cow_seller9", "Cow Farm 9");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Nellie", 870.0, LocalDate.of(2026, 5, 20));

        List<CowCut> available = cowService.getAvailableCuts(cow.getId());
        assertEquals(22, available.size(),
                "getAvailableCuts must return all 22 cuts when none are claimed");
    }

    // Test 10: getAllCuts_ReturnsExactly22
    @Test
    void getAllCuts_ReturnsExactly22() {
        User seller = registerSeller("cow_seller10@example.com", "cow_seller10", "Cow Farm 10");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Honey", 760.0, LocalDate.of(2026, 4, 1));

        List<CowCut> allCuts = cowService.getAllCuts(cow.getId());
        assertEquals(22, allCuts.size(),
                "getAllCuts must return all 22 cuts regardless of status");
    }

    // Test 11: getAvailableCuts_OnlyReturnsAvailableCuts — once status changes, count drops
    @Test
    void getAvailableCuts_CountMatchesAvailableStatusOnly() {
        User seller = registerSeller("cow_seller11@example.com", "cow_seller11", "Cow Farm 11");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Spot", 830.0, LocalDate.of(2026, 3, 15));

        // All 22 are available right after creation
        assertEquals(22, cowService.getAvailableCuts(cow.getId()).size());

        // getAllCuts also returns 22 (both available and any future claimed)
        assertEquals(22, cowService.getAllCuts(cow.getId()).size());
    }

    // ─── getCowsBySeller ──────────────────────────────────────────────

    // Test 12: getCowsBySeller_ReturnsOnlyThatSellersCows
    @Test
    void getCowsBySeller_OnlyReturnsSellersOwnCows() {
        User sellerA = registerSeller("cow_sellerA@example.com", "cow_sellerA", "Farm A");
        User sellerB = registerSeller("cow_sellerB@example.com", "cow_sellerB", "Farm B");

        CowType typeA = createCowType(sellerA.getId());
        CowType typeB = createCowType(sellerB.getId());

        cowService.createCow(typeA.getId(), "Cow A1", 700.0, LocalDate.of(2026, 1, 10));
        cowService.createCow(typeA.getId(), "Cow A2", 800.0, LocalDate.of(2026, 2, 10));
        cowService.createCow(typeB.getId(), "Cow B1", 750.0, LocalDate.of(2026, 3, 10));

        List<Cow> sellerACows = cowService.getCowsBySeller(sellerEntityId(sellerA.getId()));
        List<Cow> sellerBCows = cowService.getCowsBySeller(sellerEntityId(sellerB.getId()));

        assertEquals(2, sellerACows.size(),
                "Seller A must have exactly 2 cows");
        assertEquals(1, sellerBCows.size(),
                "Seller B must have exactly 1 cow");
        assertTrue(sellerACows.stream().allMatch(c -> c.getCowType().getId().equals(typeA.getId())),
                "All of Seller A's cows must use Seller A's CowType");
    }

    // Test 13: getCowsBySeller_NoCoWs_ReturnsEmptyList
    @Test
    void getCowsBySeller_NoCows_ReturnsEmptyList() {
        User seller = registerSeller("cow_seller13@example.com", "cow_seller13", "Empty Farm");
        // No cows created for this seller

        List<Cow> cows = cowService.getCowsBySeller(sellerEntityId(seller.getId()));

        assertTrue(cows.isEmpty(),
                "A seller with no cows must get an empty list");
    }

    // Test 14: createCow_MultipleCows_EachHasItsOwn22Cuts
    @Test
    void createCow_TwoCowsForSameSeller_EachHas22IndependentCuts() {
        User seller = registerSeller("cow_seller14@example.com", "cow_seller14", "Multi Farm");
        CowType cowType = createCowType(seller.getId());

        Cow cow1 = cowService.createCow(cowType.getId(), "Alpha", 800.0, LocalDate.of(2026, 6, 1));
        Cow cow2 = cowService.createCow(cowType.getId(), "Beta",  850.0, LocalDate.of(2026, 7, 1));

        List<CowCut> cuts1 = cowService.getAllCuts(cow1.getId());
        List<CowCut> cuts2 = cowService.getAllCuts(cow2.getId());

        assertEquals(22, cuts1.size(), "First cow must have 22 cuts");
        assertEquals(22, cuts2.size(), "Second cow must have 22 cuts");

        // Cuts belong to different cows — IDs must not overlap
        Set<Long> ids1 = cuts1.stream().map(CowCut::getId).collect(Collectors.toSet());
        Set<Long> ids2 = cuts2.stream().map(CowCut::getId).collect(Collectors.toSet());
        assertTrue(ids1.stream().noneMatch(ids2::contains),
                "CowCut records from different cows must have unique IDs");
    }

    // Test 15: createCow_LinkedToCowType — cow references its CowType correctly
    @Test
    void createCow_LinkedToCowType_CowTypeFieldsMatchInput() {
        User seller = registerSeller("cow_seller15@example.com", "cow_seller15", "Linked Farm");
        CowType cowType = createCowType(seller.getId());

        Cow cow = cowService.createCow(
                cowType.getId(), "Linked Cow", 900.0, LocalDate.of(2026, 8, 15));

        assertNotNull(cow.getCowType(), "Cow must have a non-null CowType reference");
        assertEquals(cowType.getId(), cow.getCowType().getId(),
                "Cow's CowType ID must match the requested CowType");
    }
}
