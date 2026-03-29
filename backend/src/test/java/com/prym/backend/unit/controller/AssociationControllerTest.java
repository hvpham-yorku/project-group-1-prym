package com.prym.backend.unit.controller;

import com.prym.backend.controller.AssociationController;
import com.prym.backend.model.*;
import com.prym.backend.repository.GroupMessageRepository;
import com.prym.backend.repository.GroupSellerAssociationRepository;
import com.prym.backend.repository.SellerRepository;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.AssociationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AssociationControllerTest {

    @Mock private AssociationService associationService;
    @Mock private UserRepository userRepository;
    @Mock private GroupSellerAssociationRepository associationRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private GroupMessageRepository groupMessageRepository;

    @InjectMocks
    private AssociationController associationController;

    private User testBuyerUser;
    private User testSellerUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testBuyerUser = new User();
        testBuyerUser.setId(1L);
        testBuyerUser.setEmail("buyer@example.com");
        testBuyerUser.setRole(User.Role.BUYER);

        testSellerUser = new User();
        testSellerUser.setId(2L);
        testSellerUser.setEmail("seller@example.com");
        testSellerUser.setRole(User.Role.SELLER);
    }

    private void loginAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private Map<String, Object> associationDTO() {
        Map<String, Object> dto = new HashMap<>();
        dto.put("associationId", 1L);
        dto.put("groupId", 100L);
        dto.put("sellerId", 20L);
        dto.put("status", "PENDING_ASSOCIATION");
        return dto;
    }

    // ── requestAssociation ─────────────────────────────────────────────────

    @Test
    void requestAssociation_Success_Returns200() {
        loginAs(testBuyerUser);
        when(associationService.requestAssociation(1L, 100L, 20L)).thenReturn(associationDTO());

        Map<String, Object> body = Map.of("userId", 1);
        ResponseEntity<?> response = associationController.requestAssociation(100L, 20L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("PENDING_ASSOCIATION", result.get("status"));
    }

    @Test
    void requestAssociation_Forbidden_Returns403() {
        loginAs(testBuyerUser);

        Map<String, Object> body = Map.of("userId", 99); // doesn't match logged-in user
        ResponseEntity<?> response = associationController.requestAssociation(100L, 20L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void requestAssociation_ServiceError_Returns400() {
        loginAs(testBuyerUser);
        when(associationService.requestAssociation(1L, 100L, 20L))
                .thenThrow(new RuntimeException("Group already has a pending or active association."));

        Map<String, Object> body = Map.of("userId", 1);
        ResponseEntity<?> response = associationController.requestAssociation(100L, 20L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertTrue(result.get("error").toString().contains("pending or active association"));
    }

    // ── cancelAssociation ──────────────────────────────────────────────────

    @Test
    void cancelAssociation_Success_Returns200() {
        loginAs(testBuyerUser);
        when(associationService.cancelAssociation(1L, 100L))
                .thenReturn(Map.of("message", "Association request cancelled."));

        Map<String, Object> body = Map.of("userId", 1);
        ResponseEntity<?> response = associationController.cancelAssociation(100L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("Association request cancelled.", result.get("message"));
    }

    @Test
    void cancelAssociation_Forbidden_Returns403() {
        loginAs(testBuyerUser);

        Map<String, Object> body = Map.of("userId", 99);
        ResponseEntity<?> response = associationController.cancelAssociation(100L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── requestDisassociation ──────────────────────────────────────────────

    @Test
    void requestDisassociation_Success_Returns200() {
        loginAs(testBuyerUser);
        Map<String, Object> dto = Map.of("status", "PENDING_DISASSOCIATION");
        when(associationService.requestDisassociation(1L, 100L)).thenReturn(dto);

        Map<String, Object> body = Map.of("userId", 1);
        ResponseEntity<?> response = associationController.requestDisassociation(100L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("PENDING_DISASSOCIATION", result.get("status"));
    }

    @Test
    void requestDisassociation_Forbidden_Returns403() {
        loginAs(testBuyerUser);

        Map<String, Object> body = Map.of("userId", 99);
        ResponseEntity<?> response = associationController.requestDisassociation(100L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── getGroupAssociation ────────────────────────────────────────────────

    @Test
    void getGroupAssociation_WithActiveAssociation_Returns200WithData() {
        loginAs(testBuyerUser);
        Map<String, Object> dto = Map.of("status", "ASSOCIATED", "sellerId", 20L);
        when(associationService.getGroupAssociation(1L, 100L)).thenReturn(dto);

        ResponseEntity<?> response = associationController.getGroupAssociation(100L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("ASSOCIATED", result.get("status"));
    }

    @Test
    void getGroupAssociation_NoActiveAssociation_Returns200WithEmptyMap() {
        loginAs(testBuyerUser);
        when(associationService.getGroupAssociation(1L, 100L)).thenReturn(null);

        ResponseEntity<?> response = associationController.getGroupAssociation(100L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((Map<?, ?>) response.getBody()).isEmpty());
    }

    @Test
    void getGroupAssociation_Forbidden_Returns403() {
        loginAs(testBuyerUser);

        ResponseEntity<?> response = associationController.getGroupAssociation(100L, 99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── getSellerPendingRequests ───────────────────────────────────────────

    @Test
    void getSellerPendingRequests_Success_Returns200() {
        loginAs(testSellerUser);
        List<Map<String, Object>> pending = List.of(Map.of("associationId", 1L));
        when(associationService.getSellerPendingRequests(2L)).thenReturn(pending);

        ResponseEntity<?> response = associationController.getSellerPendingRequests(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> result = (List<?>) response.getBody();
        assertEquals(1, result.size());
    }

    @Test
    void getSellerPendingRequests_Forbidden_Returns403() {
        loginAs(testSellerUser);

        ResponseEntity<?> response = associationController.getSellerPendingRequests(99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── getSellerAssociations ──────────────────────────────────────────────

    @Test
    void getSellerAssociations_Success_Returns200() {
        loginAs(testSellerUser);
        when(associationService.getSellerAssociations(2L))
                .thenReturn(List.of(Map.of("status", "ASSOCIATED")));

        ResponseEntity<?> response = associationController.getSellerAssociations(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getSellerAssociations_Forbidden_Returns403() {
        loginAs(testSellerUser);

        ResponseEntity<?> response = associationController.getSellerAssociations(99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── respondToAssociation ──────────────────────────────────────────────

    @Test
    void respondToAssociation_Approve_Returns200() {
        loginAs(testSellerUser);
        Map<String, Object> dto = Map.of("status", "ASSOCIATED");
        when(associationService.respondToAssociation(2L, 1L, "APPROVE", null)).thenReturn(dto);

        Map<String, Object> body = new HashMap<>();
        body.put("userId", 2);
        body.put("action", "APPROVE");
        ResponseEntity<?> response = associationController.respondToAssociation(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("ASSOCIATED", result.get("status"));
    }

    @Test
    void respondToAssociation_Deny_Returns200WithDeniedStatus() {
        loginAs(testSellerUser);
        Map<String, Object> dto = Map.of("status", "DENIED");
        when(associationService.respondToAssociation(2L, 1L, "DENY", "Not available"))
                .thenReturn(dto);

        Map<String, Object> body = new HashMap<>();
        body.put("userId", 2);
        body.put("action", "DENY");
        body.put("note", "Not available");
        ResponseEntity<?> response = associationController.respondToAssociation(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("DENIED", result.get("status"));
    }

    @Test
    void respondToAssociation_Forbidden_Returns403() {
        loginAs(testSellerUser);

        Map<String, Object> body = Map.of("userId", 99, "action", "APPROVE");
        ResponseEntity<?> response = associationController.respondToAssociation(1L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── respondToDisassociation ────────────────────────────────────────────

    @Test
    void respondToDisassociation_Confirm_Returns200() {
        loginAs(testSellerUser);
        Map<String, Object> dto = Map.of("status", "DISASSOCIATED");
        when(associationService.respondToDisassociation(2L, 1L, "CONFIRM")).thenReturn(dto);

        Map<String, Object> body = Map.of("userId", 2, "action", "CONFIRM");
        ResponseEntity<?> response = associationController.respondToDisassociation(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals("DISASSOCIATED", result.get("status"));
    }

    @Test
    void respondToDisassociation_Deny_Returns200WithAssociatedStatus() {
        loginAs(testSellerUser);
        Map<String, Object> dto = Map.of("status", "ASSOCIATED");
        when(associationService.respondToDisassociation(2L, 1L, "DENY")).thenReturn(dto);

        Map<String, Object> body = Map.of("userId", 2, "action", "DENY");
        ResponseEntity<?> response = associationController.respondToDisassociation(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void respondToDisassociation_Forbidden_Returns403() {
        loginAs(testSellerUser);

        Map<String, Object> body = Map.of("userId", 99, "action", "CONFIRM");
        ResponseEntity<?> response = associationController.respondToDisassociation(1L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── getGroupMessages (seller-side) ─────────────────────────────────────

    @Test
    void getGroupMessages_Success_Returns200WithMessages() {
        loginAs(testSellerUser);

        Seller seller = new Seller();
        seller.setId(20L);
        seller.setUser(testSellerUser);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));

        BuyerGroup group = new BuyerGroup();
        group.setId(100L);

        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.ASSOCIATED);
        assoc.setGroup(group);
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));
        when(groupMessageRepository.findTop50ByGroupIdOrderBySentAtAsc(100L))
                .thenReturn(List.of());

        ResponseEntity<?> response = associationController.getGroupMessages(1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertEquals(100L, result.get("groupId"));
    }

    @Test
    void getGroupMessages_Forbidden_WhenUserIdMismatch_Returns403() {
        loginAs(testSellerUser);

        ResponseEntity<?> response = associationController.getGroupMessages(1L, 99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getGroupMessages_NotActiveAssociation_Returns403() {
        loginAs(testSellerUser);

        Seller seller = new Seller();
        seller.setId(20L);
        seller.setUser(testSellerUser);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(seller));

        BuyerGroup group = new BuyerGroup();
        group.setId(100L);

        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.DENIED); // terminal/inactive status
        assoc.setGroup(group);
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        ResponseEntity<?> response = associationController.getGroupMessages(1L, 2L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertTrue(result.get("error").toString().contains("Not an active association"));
    }

    @Test
    void getGroupMessages_AssociationBelongsToDifferentSeller_Returns403() {
        loginAs(testSellerUser);

        Seller mySeller = new Seller();
        mySeller.setId(20L);
        mySeller.setUser(testSellerUser);
        when(sellerRepository.findByUserId(2L)).thenReturn(Optional.of(mySeller));

        Seller otherSeller = new Seller();
        otherSeller.setId(99L);

        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setId(1L);
        assoc.setSeller(otherSeller); // belongs to a different seller
        assoc.setStatus(AssociationStatus.ASSOCIATED);
        when(associationRepository.findById(1L)).thenReturn(Optional.of(assoc));

        ResponseEntity<?> response = associationController.getGroupMessages(1L, 2L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
