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