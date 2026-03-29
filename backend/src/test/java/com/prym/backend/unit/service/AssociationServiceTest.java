package com.prym.backend.unit.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import com.prym.backend.service.AssociationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssociationServiceTest {

    @Mock private GroupSellerAssociationRepository associationRepository;
    @Mock private BuyerGroupRepository groupRepository;
    @Mock private BuyerRepository buyerRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private BuyerGroupMemberRepository memberRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AssociationService associationService;

    private User buyerUser;
    private User sellerUser;
    private Buyer buyer;
    private Seller seller;
    private BuyerGroup group;

    @BeforeEach
    void setUp() {
        buyerUser = new User();
        buyerUser.setId(1L);
        buyerUser.setFirstName("Alice");
        buyerUser.setLastName("Smith");

        buyer = new Buyer();
        buyer.setId(10L);
        buyer.setUser(buyerUser);

        sellerUser = new User();
        sellerUser.setId(2L);
        sellerUser.setFirstName("Bob");
        sellerUser.setLastName("Jones");

        seller = new Seller();
        seller.setId(20L);
        seller.setShopName("Bob's Farm");
        seller.setUser(sellerUser);

        group = new BuyerGroup();
        group.setId(100L);
        group.setName("Halal Crew");
        group.setCreator(buyer);
    }

    // ── requestAssociation ─────────────────────────────────────────────────

    @Test
    void requestAssociation_Success_ReturnsPendingDTO() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(sellerRepository.findById(20L)).thenReturn(Optional.of(seller));
        when(associationRepository.findByGroupIdAndStatusInForUpdate(eq(100L), anyList()))
                .thenReturn(Optional.empty());
        when(associationRepository.findTopByGroupIdAndSellerIdOrderByRequestedAtDesc(100L, 20L))
                .thenReturn(Optional.empty());
        when(associationRepository.save(any(GroupSellerAssociation.class)))
                .thenAnswer(inv -> {
                    GroupSellerAssociation a = inv.getArgument(0);
                    a.setId(1L);
                    return a;
                });

        Map<String, Object> result = associationService.requestAssociation(1L, 100L, 20L);

        assertEquals("PENDING_ASSOCIATION", result.get("status"));
        assertEquals(100L, result.get("groupId"));
        assertEquals(20L, result.get("sellerId"));
        verify(messagingTemplate).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void requestAssociation_BuyerNotFound_Throws() {
        when(buyerRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> associationService.requestAssociation(99L, 100L, 20L));
    }

    @Test
    void requestAssociation_GroupNotFound_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> associationService.requestAssociation(1L, 999L, 20L));
    }

    @Test
    void requestAssociation_SellerNotFound_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(sellerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> associationService.requestAssociation(1L, 100L, 999L));
    }

    @Test
    void requestAssociation_NotGroupCreator_Throws() {
        Buyer otherBuyer = new Buyer();
        otherBuyer.setId(99L);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(otherBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(sellerRepository.findById(20L)).thenReturn(Optional.of(seller));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> associationService.requestAssociation(1L, 100L, 20L));
        assertTrue(ex.getMessage().contains("creator"));
    }

    @Test
    void requestAssociation_AlreadyHasActiveAssociation_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(sellerRepository.findById(20L)).thenReturn(Optional.of(seller));
        GroupSellerAssociation existing = new GroupSellerAssociation();
        existing.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(associationRepository.findByGroupIdAndStatusInForUpdate(eq(100L), anyList()))
                .thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class,
                () -> associationService.requestAssociation(1L, 100L, 20L));
    }

    @Test
    void requestAssociation_Within24HrLockout_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(sellerRepository.findById(20L)).thenReturn(Optional.of(seller));
        when(associationRepository.findByGroupIdAndStatusInForUpdate(eq(100L), anyList()))
                .thenReturn(Optional.empty());

        GroupSellerAssociation denied = new GroupSellerAssociation();
        denied.setStatus(AssociationStatus.DENIED);
        denied.setDeniedAt(LocalDateTime.now().minusHours(2)); // only 2 hrs ago
        when(associationRepository.findTopByGroupIdAndSellerIdOrderByRequestedAtDesc(100L, 20L))
                .thenReturn(Optional.of(denied));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> associationService.requestAssociation(1L, 100L, 20L));
        assertTrue(ex.getMessage().contains("24 hours"));
    }

    // ── cancelAssociation ──────────────────────────────────────────────────

    @Test
    void cancelAssociation_Success_ReturnsCancelledMessage() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        GroupSellerAssociation pending = new GroupSellerAssociation();
        pending.setId(1L);
        pending.setGroup(group);
        pending.setSeller(seller);
        pending.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(associationRepository.findByGroupIdAndStatusInForUpdate(
                eq(100L), eq(List.of(AssociationStatus.PENDING_ASSOCIATION))))
                .thenReturn(Optional.of(pending));

        Map<String, Object> result = associationService.cancelAssociation(1L, 100L);

        assertEquals("Association request cancelled.", result.get("message"));
        verify(associationRepository).save(pending);
        assertEquals(AssociationStatus.CANCELLED, pending.getStatus());
    }

    @Test
    void cancelAssociation_NotGroupCreator_Throws() {
        Buyer otherBuyer = new Buyer();
        otherBuyer.setId(99L);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(otherBuyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));

        assertThrows(RuntimeException.class,
                () -> associationService.cancelAssociation(1L, 100L));
    }

    @Test
    void cancelAssociation_NoPendingRequest_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(associationRepository.findByGroupIdAndStatusInForUpdate(
                eq(100L), eq(List.of(AssociationStatus.PENDING_ASSOCIATION))))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> associationService.cancelAssociation(1L, 100L));
    }

    // ── requestDisassociation ──────────────────────────────────────────────

    @Test
    void requestDisassociation_Success_SetsPendingDisassociation() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.ASSOCIATED);
        when(associationRepository.findByGroupIdAndStatusInForUpdate(
                eq(100L), eq(List.of(AssociationStatus.ASSOCIATED))))
                .thenReturn(Optional.of(assoc));
        when(associationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = associationService.requestDisassociation(1L, 100L);

        assertEquals("PENDING_DISASSOCIATION", result.get("status"));
        assertEquals(AssociationStatus.PENDING_DISASSOCIATION, assoc.getStatus());
    }

    @Test
    void requestDisassociation_GroupNotAssociated_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(associationRepository.findByGroupIdAndStatusInForUpdate(
                eq(100L), eq(List.of(AssociationStatus.ASSOCIATED))))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> associationService.requestDisassociation(1L, 100L));
    }

    // ── getGroupAssociation ────────────────────────────────────────────────

    @Test
    void getGroupAssociation_AsMember_ReturnsAssociationDTO() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(memberRepository.existsByGroupIdAndBuyerId(100L, 10L)).thenReturn(true);
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.ASSOCIATED);
        when(associationRepository.findByGroupIdAndStatusIn(eq(100L), anyList()))
                .thenReturn(Optional.of(assoc));

        Map<String, Object> result = associationService.getGroupAssociation(1L, 100L);

        assertNotNull(result);
        assertEquals("ASSOCIATED", result.get("status"));
    }

    @Test
    void getGroupAssociation_NotMember_Throws() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(memberRepository.existsByGroupIdAndBuyerId(100L, 10L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> associationService.getGroupAssociation(1L, 100L));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void getGroupAssociation_NoActiveAssociation_ReturnsNull() {
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(memberRepository.existsByGroupIdAndBuyerId(100L, 10L)).thenReturn(true);
        when(associationRepository.findByGroupIdAndStatusIn(eq(100L), anyList()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = associationService.getGroupAssociation(1L, 100L);

        assertNull(result);
    }

    // ── getSellerPendingRequests ───────────────────────────────────────────

    @Test
    void getSellerPendingRequests_ReturnsMappedList() {
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(associationRepository.findBySellerIdAndStatus(20L, AssociationStatus.PENDING_ASSOCIATION))
                .thenReturn(List.of(assoc));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of());

        List<Map<String, Object>> result = associationService.getSellerPendingRequests(2L);

        assertEquals(1, result.size());
        assertEquals("PENDING_ASSOCIATION", result.get(0).get("status"));
    }

    @Test
    void getSellerPendingRequests_SellerNotFound_Throws() {
        when(sellerRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> associationService.getSellerPendingRequests(99L));
    }

    // ── getSellerAssociations ──────────────────────────────────────────────

    @Test
    void getSellerAssociations_ReturnsActiveAndPendingDisassociation() {
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.ASSOCIATED);
        when(associationRepository.findBySellerIdAndStatusIn(
                eq(20L), anyList())).thenReturn(List.of(assoc));
        when(memberRepository.findByGroupId(100L)).thenReturn(List.of());

        List<Map<String, Object>> result = associationService.getSellerAssociations(2L);

        assertEquals(1, result.size());
        assertEquals("ASSOCIATED", result.get(0).get("status"));
    }

    // ── respondToAssociation ──────────────────────────────────────────────

    @Test
    void respondToAssociation_Approve_SetsAssociatedAndUpdatesGroup() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));
        when(associationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = associationService.respondToAssociation(2L, 1L, "APPROVE", null);

        assertEquals("ASSOCIATED", result.get("status"));
        assertEquals(AssociationStatus.ASSOCIATED, assoc.getStatus());
        assertEquals(seller, group.getAssociatedSeller());
    }

    @Test
    void respondToAssociation_Deny_SetsDeniedAndRecordsDeniedAt() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));
        when(associationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = associationService.respondToAssociation(2L, 1L, "DENY", "Not interested");

        assertEquals("DENIED", result.get("status"));
        assertNotNull(assoc.getDeniedAt());
    }

    @Test
    void respondToAssociation_InvalidAction_Throws() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> associationService.respondToAssociation(2L, 1L, "INVALID", null));
        assertTrue(ex.getMessage().contains("Invalid action"));
    }

    @Test
    void respondToAssociation_NotSellersRequest_Throws() {
        Seller otherSeller = new Seller();
        otherSeller.setId(99L);
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(otherSeller); // belongs to a different seller
        assoc.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        assertThrows(RuntimeException.class,
                () -> associationService.respondToAssociation(2L, 1L, "APPROVE", null));
    }

    @Test
    void respondToAssociation_NotPending_Throws() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.ASSOCIATED); // not pending
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> associationService.respondToAssociation(2L, 1L, "APPROVE", null));
        assertTrue(ex.getMessage().contains("no longer pending"));
    }

    // ── respondToDisassociation ────────────────────────────────────────────

    @Test
    void respondToDisassociation_Confirm_SetsDisassociatedAndClearsGroup() {
        group.setAssociatedSeller(seller);
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_DISASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));
        when(associationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = associationService.respondToDisassociation(2L, 1L, "CONFIRM");

        assertEquals("DISASSOCIATED", result.get("status"));
        assertNull(group.getAssociatedSeller());
    }

    @Test
    void respondToDisassociation_Deny_StaysAssociated() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_DISASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));
        when(associationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = associationService.respondToDisassociation(2L, 1L, "DENY");

        assertEquals("ASSOCIATED", result.get("status"));
    }

    @Test
    void respondToDisassociation_InvalidAction_Throws() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_DISASSOCIATION);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> associationService.respondToDisassociation(2L, 1L, "INVALID"));
        assertTrue(ex.getMessage().contains("Invalid action"));
    }

    @Test
    void respondToDisassociation_NotPendingDisassociation_Throws() {
        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.ASSOCIATED); // wrong status
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        assertThrows(RuntimeException.class,
                () -> associationService.respondToDisassociation(2L, 1L, "CONFIRM"));
    }
}
