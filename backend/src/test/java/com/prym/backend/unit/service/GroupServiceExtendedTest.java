package com.prym.backend.unit.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import com.prym.backend.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Extended unit tests for GroupService — covering methods not tested in GroupServiceTest:
 * - getGroupByCode
 * - regenerateInviteCode
 * - getMatchingFarms
 * - parseCuts edge cases (via saveCuts)
 * - saveCuts quantity boundary validation (qty=0, qty=3)
 * - leaveGroup creator-transfer (creator leaves while others remain)
 */
@ExtendWith(MockitoExtension.class)
public class GroupServiceExtendedTest {

    @Mock private BuyerGroupRepository groupRepository;
    @Mock private BuyerGroupMemberRepository memberRepository;
    @Mock private BuyerRepository buyerRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private User buyerUser1;
    private Buyer buyer1;
    private BuyerGroup testGroup;

    @BeforeEach
    void setUp() {
        buyerUser1 = new User();
        buyerUser1.setId(1L);
        buyerUser1.setEmail("buyer1@example.com");
        buyerUser1.setFirstName("Alice");
        buyerUser1.setRole(User.Role.BUYER);

        buyer1 = new Buyer();
        buyer1.setId(10L);
        buyer1.setUser(buyerUser1);

        testGroup = new BuyerGroup();
        testGroup.setId(100L);
        testGroup.setName("Test Group");
        testGroup.setCreator(buyer1);
        testGroup.setInviteCode("ABCD1234");
        testGroup.setCertifications("HALAL");
    }

    private BuyerGroupMember makeMember(BuyerGroup group, Buyer buyer, String cuts) {
        BuyerGroupMember m = new BuyerGroupMember();
        m.setGroup(group);
        m.setBuyer(buyer);
        m.setClaimedCuts(cuts);
        return m;
    }

    // ─── getGroupByCode ────────────────────────────────────────────────

    // Test 1: getGroupByCode_Success — valid code → returns group DTO
    @Test
    void getGroupByCode_ValidCode_ReturnsGroupDTO() {
        when(groupRepository.findByInviteCode("ABCD1234"))
                .thenReturn(Optional.of(testGroup));
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        BuyerGroupMember membership = makeMember(testGroup, buyer1, null);
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));

        Map<String, Object> result = groupService.getGroupByCode(1L, "ABCD1234");

        assertNotNull(result);
        assertEquals(100L, result.get("groupId"));
        assertEquals("Test Group", result.get("groupName"));
    }

    // Test 2: getGroupByCode_CodeNotFound — no group with that code → throws
    @Test
    void getGroupByCode_InvalidCode_Throws() {
        when(groupRepository.findByInviteCode("XXXXXXXX"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.getGroupByCode(1L, "XXXXXXXX"));
        assertEquals("No group found with that code.", ex.getMessage());
    }

    // Test 3: getGroupByCode_TrimsAndUppercasesCode — "abcd1234" → searches for "ABCD1234"
    @Test
    void getGroupByCode_LowercaseCode_NormalizedBeforeLookup() {
        when(groupRepository.findByInviteCode("ABCD1234"))
                .thenReturn(Optional.of(testGroup));
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(makeMember(testGroup, buyer1, null)));

        Map<String, Object> result = groupService.getGroupByCode(1L, "abcd1234");

        assertNotNull(result);
        verify(groupRepository).findByInviteCode("ABCD1234");
    }

    // Test 4: getGroupByCode_CodeWithLeadingSpaces — "  ABCD1234  " → trimmed and uppercased
    @Test
    void getGroupByCode_CodeWithSpaces_TrimmedBeforeLookup() {
        when(groupRepository.findByInviteCode("ABCD1234"))
                .thenReturn(Optional.of(testGroup));
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(makeMember(testGroup, buyer1, null)));

        groupService.getGroupByCode(1L, "  ABCD1234  ");

        verify(groupRepository).findByInviteCode("ABCD1234");
    }

    // ─── regenerateInviteCode ─────────────────────────────────────────

    // Test 5: regenerateInviteCode_Success_AsCreator — creator regenerates → new code saved
    @Test
    void regenerateInviteCode_AsCreator_SavesNewCode() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(BuyerGroup.class))).thenReturn(testGroup);
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(makeMember(testGroup, buyer1, null)));

        Map<String, Object> result = groupService.regenerateInviteCode(1L, 100L);

        assertNotNull(result);
        verify(groupRepository).save(any(BuyerGroup.class));
    }

    // Test 6: regenerateInviteCode_NewCodeIsDifferentFromOld — the invite code actually changes
    @Test
    void regenerateInviteCode_ProducesNewCode() {
        String oldCode = testGroup.getInviteCode(); // "ABCD1234"
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(groupRepository.save(any(BuyerGroup.class))).thenAnswer(inv -> {
            BuyerGroup saved = inv.getArgument(0);
            // Just return it; the service sets a new code before save
            return saved;
        });
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(makeMember(testGroup, buyer1, null)));

        groupService.regenerateInviteCode(1L, 100L);

        // The group's invite code should have been mutated before save
        assertNotEquals(oldCode, testGroup.getInviteCode(),
                "Invite code must be changed after regeneration");
    }

    // Test 7: regenerateInviteCode_NotCreator — another buyer tries → throws
    @Test
    void regenerateInviteCode_NotCreator_Throws() {
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Bob");
        Buyer buyer2 = new Buyer();
        buyer2.setId(20L);
        buyer2.setUser(user2);

        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer2));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        // testGroup creator is buyer1 (id=10), buyer2 (id=20) is not the creator

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.regenerateInviteCode(2L, 100L));
        assertEquals("Only the group creator can regenerate the invite code.", ex.getMessage());
        verify(groupRepository, never()).save(any());
    }

    // Test 8: regenerateInviteCode_GroupNotFound — group doesn't exist → throws
    @Test
    void regenerateInviteCode_GroupNotFound_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> groupService.regenerateInviteCode(1L, 999L));
    }

    // ─── getMatchingFarms ─────────────────────────────────────────────

    // Test 9: getMatchingFarms_NoCertifications_AllSellersArePerFectMatches
    // Group with no certification requirements → every seller is a "perfect match"
    @Test
    void getMatchingFarms_NoCerts_AllSellersArePerfectMatches() {
        testGroup.setCertifications(""); // no certs required

        Seller seller = buildSeller(1L, "Farm One", 43.0, -79.0, new ArrayList<>());

        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser1));
        when(sellerRepository.findAll()).thenReturn(List.of(seller));

        Map<String, Object> result = groupService.getMatchingFarms(1L, 100L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perfect = (List<Map<String, Object>>) result.get("perfectMatches");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partial = (List<Map<String, Object>>) result.get("partialMatches");

        assertEquals(1, perfect.size(), "All sellers should be perfect matches with no certs");
        assertTrue(partial.isEmpty(), "No partial matches when no certs required");
        assertEquals("Farm One", perfect.get(0).get("shopName"));
    }

    // Test 10: getMatchingFarms_SellerHasAllRequiredCerts_IsPerfectMatch
    @Test
    void getMatchingFarms_SellerHasAllRequiredCerts_PerfectMatch() {
        testGroup.setCertifications("HALAL");

        Certification halalCert = new Certification();
        halalCert.setName(Certification.CertificationType.HALAL);

        Seller seller = buildSeller(1L, "Halal Farm", null, null,
                new ArrayList<>(List.of(halalCert)));

        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser1));
        when(sellerRepository.findAll()).thenReturn(List.of(seller));

        Map<String, Object> result = groupService.getMatchingFarms(1L, 100L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perfect = (List<Map<String, Object>>) result.get("perfectMatches");
        assertEquals(1, perfect.size());
        assertEquals("Halal Farm", perfect.get(0).get("shopName"));
        assertEquals(1, perfect.get(0).get("matchCount"));
    }

    // Test 11: getMatchingFarms_SellerHasSomeButNotAllCerts_IsPartialMatch
    @Test
    void getMatchingFarms_SellerMissesOneCert_PartialMatch() {
        testGroup.setCertifications("HALAL,ORGANIC");

        Certification halalCert = new Certification();
        halalCert.setName(Certification.CertificationType.HALAL);
        // Seller only has HALAL, not ORGANIC

        Seller seller = buildSeller(1L, "Partial Farm", null, null,
                new ArrayList<>(List.of(halalCert)));

        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser1));
        when(sellerRepository.findAll()).thenReturn(List.of(seller));

        Map<String, Object> result = groupService.getMatchingFarms(1L, 100L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perfect = (List<Map<String, Object>>) result.get("perfectMatches");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partial = (List<Map<String, Object>>) result.get("partialMatches");

        assertTrue(perfect.isEmpty(), "Partial seller should not be in perfectMatches");
        assertEquals(1, partial.size());
        assertEquals("Partial Farm", partial.get(0).get("shopName"));
        assertEquals(1, partial.get(0).get("matchCount"));
        assertEquals(2, partial.get(0).get("totalRequired"));
    }

    // Test 12: getMatchingFarms_SellerHasNoCerts_Excluded
    @Test
    void getMatchingFarms_SellerHasNoCertsAndGroupRequiresSome_Excluded() {
        testGroup.setCertifications("HALAL");

        Seller seller = buildSeller(1L, "Uncertified Farm", null, null, new ArrayList<>());

        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser1));
        when(sellerRepository.findAll()).thenReturn(List.of(seller));

        Map<String, Object> result = groupService.getMatchingFarms(1L, 100L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perfect = (List<Map<String, Object>>) result.get("perfectMatches");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partial = (List<Map<String, Object>>) result.get("partialMatches");

        assertTrue(perfect.isEmpty(), "Uncertified seller should not appear in perfect matches");
        assertTrue(partial.isEmpty(), "Zero overlap → excluded entirely");
    }

    // Test 13: getMatchingFarms_MultipleSellers_SortedByDistance
    @Test
    void getMatchingFarms_PerfectMatches_SortedByDistanceAscending() {
        testGroup.setCertifications(""); // no certs → all are perfect

        buyerUser1.setLatitude(43.7);
        buyerUser1.setLongitude(-79.4);

        // Seller A is far (lat=50.0), Seller B is close (lat=43.8)
        Seller sellerA = buildSeller(1L, "Far Farm",   50.0, -79.4, new ArrayList<>());
        Seller sellerB = buildSeller(2L, "Close Farm", 43.8, -79.4, new ArrayList<>());

        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser1));
        when(sellerRepository.findAll()).thenReturn(List.of(sellerA, sellerB));

        Map<String, Object> result = groupService.getMatchingFarms(1L, 100L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perfect = (List<Map<String, Object>>) result.get("perfectMatches");
        assertEquals(2, perfect.size());
        assertEquals("Close Farm", perfect.get(0).get("shopName"),
                "Closer farm must appear first");
        assertEquals("Far Farm", perfect.get(1).get("shopName"));
    }

    // Test 14: getMatchingFarms_GroupNotFound — throws
    @Test
    void getMatchingFarms_GroupNotFound_Throws() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> groupService.getMatchingFarms(1L, 999L));
    }

    // Test 15: getMatchingFarms_DistanceIsNullWhenCoordinatesMissing
    // Sellers with no lat/lon get "N/A" distance and null distance value
    @Test
    void getMatchingFarms_MissingCoordinates_DistanceIsNull() {
        testGroup.setCertifications("");
        buyerUser1.setLatitude(null); // buyer has no coordinates
        buyerUser1.setLongitude(null);

        Seller seller = buildSeller(1L, "Coord-less Farm", null, null, new ArrayList<>());

        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyerUser1));
        when(sellerRepository.findAll()).thenReturn(List.of(seller));

        Map<String, Object> result = groupService.getMatchingFarms(1L, 100L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perfect = (List<Map<String, Object>>) result.get("perfectMatches");
        assertNull(perfect.get(0).get("distance"),
                "distance must be null when coordinates are missing");
        assertEquals("N/A", perfect.get(0).get("distanceFormatted"));
    }

    // ─── saveCuts — parseCuts edge cases and quantity boundaries ──────

    // Test 16: saveCuts_QuantityZero_Throws — "Chuck x0" is invalid
    @Test
    void saveCuts_QuantityZero_Throws() {
        BuyerGroupMember membership = makeMember(testGroup, buyer1, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L))
                .thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(1L, 100L, "Chuck x0"));
        assertTrue(ex.getMessage().contains("Quantity for 'Chuck' must be 1 or 2"));
    }

    // Test 17: saveCuts_QuantityThree_Throws — "Rib x3" exceeds MAX_QTY_PER_CUT (2)
    @Test
    void saveCuts_QuantityThree_Throws() {
        BuyerGroupMember membership = makeMember(testGroup, buyer1, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L))
                .thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(1L, 100L, "Rib x3"));
        assertTrue(ex.getMessage().contains("Quantity for 'Rib' must be 1 or 2"));
    }

    // Test 18: saveCuts_MultipleCutsOneCausesConflict — first cut is fine but second is taken
    @Test
    void saveCuts_SecondCutIsFullyTaken_Throws() {
        // Another buyer already claimed both Sirloin slots
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Bob");
        Buyer buyer2 = new Buyer();
        buyer2.setId(20L);
        buyer2.setUser(user2);
        BuyerGroupMember otherMember = makeMember(testGroup, buyer2, "Sirloin x2");

        BuyerGroupMember membership = makeMember(testGroup, buyer1, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L))
                .thenReturn(Optional.of(membership));
        // Both members returned — "others" = otherMember only (buyer1 is filtered out)
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(membership, otherMember));

        // Chuck is fine, Sirloin is full (x2 already taken by Bob)
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> groupService.saveCuts(1L, 100L, "Chuck, Sirloin"));
        assertTrue(ex.getMessage().contains("'Sirloin' only has 0 slot(s) left"));
    }

    // Test 19: saveCuts_ParsesCutsWithMultiplier — "Chuck x2, Rib" → Chuck=2, Rib=1
    @Test
    void saveCuts_ParsesCutsWithMultiplier_Correctly() {
        BuyerGroupMember membership = makeMember(testGroup, buyer1, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L))
                .thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));
        when(memberRepository.save(any(BuyerGroupMember.class))).thenReturn(membership);
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

        // No conflict (buyer is the only member), so this should succeed
        groupService.saveCuts(1L, 100L, "Chuck x2, Rib");

        assertEquals("Chuck x2, Rib", membership.getClaimedCuts());
    }

    // Test 20: saveCuts_TrimsClaimedCutsString — leading/trailing whitespace is trimmed on save
    @Test
    void saveCuts_TrimsWhitespaceFromCutsString() {
        BuyerGroupMember membership = makeMember(testGroup, buyer1, null);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L))
                .thenReturn(Optional.of(membership));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of(membership));
        when(memberRepository.save(any(BuyerGroupMember.class))).thenReturn(membership);
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));

        groupService.saveCuts(1L, 100L, "  Chuck  ");

        assertEquals("Chuck", membership.getClaimedCuts(),
                "Saved cuts string must be trimmed");
    }

    // ─── leaveGroup — creator transfer ────────────────────────────────

    // Test 21: leaveGroup_CreatorLeaves_CreatorTransferredToLowestIdMember
    @Test
    void leaveGroup_CreatorLeaves_TransfersCreatorToLowestIdMember() {
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Bob");
        Buyer buyer2 = new Buyer();
        buyer2.setId(20L);
        buyer2.setUser(user2);

        BuyerGroupMember creatorMembership = makeMember(testGroup, buyer1, null);
        creatorMembership.setId(1L); // creator joined first (lower ID)

        BuyerGroupMember member2 = makeMember(testGroup, buyer2, null);
        member2.setId(2L);

        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer1));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 10L))
                .thenReturn(Optional.of(creatorMembership));
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(member2)); // one member remains after leave

        groupService.leaveGroup(1L, 100L);

        // Group must not be deleted
        verify(groupRepository, never()).deleteById(any());
        // Group must be saved with new creator = buyer2
        verify(groupRepository).save(argThat(g -> g.getCreator().getId().equals(20L)));
    }

    // Test 22: leaveGroup_NonCreatorLeaves_CreatorUnchanged
    @Test
    void leaveGroup_NonCreatorLeaves_CreatorUnchanged() {
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Bob");
        Buyer buyer2 = new Buyer();
        buyer2.setId(20L);
        buyer2.setUser(user2);

        // testGroup creator = buyer1; buyer2 is a regular member
        BuyerGroupMember membership2 = makeMember(testGroup, buyer2, null);

        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer2));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(testGroup));
        when(memberRepository.findByGroupIdAndBuyerId(100L, 20L))
                .thenReturn(Optional.of(membership2));
        // creator (buyer1) still remains after buyer2 leaves
        when(memberRepository.findByGroupId(100L))
                .thenReturn(List.of(makeMember(testGroup, buyer1, null)));

        groupService.leaveGroup(2L, 100L);

        // Group must not be deleted
        verify(groupRepository, never()).deleteById(any());
        // groupRepository.save() must NOT be called because the creator didn't change
        verify(groupRepository, never()).save(any());
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Seller buildSeller(Long id, String shopName, Double lat, Double lon,
                               List<Certification> certs) {
        User sellerUser = new User();
        sellerUser.setId(id + 100);
        sellerUser.setEmail("seller" + id + "@farm.com");
        sellerUser.setFirstName("Seller");
        sellerUser.setLastName(shopName);
        sellerUser.setLatitude(lat);
        sellerUser.setLongitude(lon);

        Seller seller = new Seller();
        seller.setId(id);
        seller.setUser(sellerUser);
        seller.setShopName(shopName);
        seller.setCertifications(certs);
        return seller;
    }
}
