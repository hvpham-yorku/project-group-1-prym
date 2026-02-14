package com.prym.backend.controller;

import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.service.SellerService;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class SellerControllerTest {

    @Mock
    private SellerService sellerService;

    @Mock
    private SessionService sessionService;

    private SellerController sellerController;

    private User testUser;
    private Seller testSeller;
    private String validSessionId = "valid-session-id";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sellerController = new SellerController(sellerService, sessionService);

        // Setup a User
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("seller1");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("seller@example.com");
        testUser.setPhoneNumber("1234567890");
        testUser.setRole(User.Role.SELLER);

        // Setup Seller linked to User
        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setShopName("Old Shop");
        testSeller.setShopAddress("Old Address");
        testSeller.setUser(testUser);

        // Mock session validation to return the test user
        when(sessionService.validateSession(validSessionId)).thenReturn(Optional.of(testUser));
    }

    @Test
    void createSeller_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "1");
        request.put("shopName", "New Shop");
        request.put("shopAddress", "123 Main Street");

        when(sellerService.createSellerProfile(1L, "New Shop", "123 Main Street"))
                .thenReturn(testSeller);

        ResponseEntity<?> response = sellerController.createSeller(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testSeller, response.getBody());
    }

    @Test
    void getSeller_Success() {
        when(sellerService.getSellerProfile(1L)).thenReturn(testSeller);

        ResponseEntity<?> response = sellerController.getSeller(1L, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("John", body.get("firstName"));
        assertEquals("Doe", body.get("lastName"));
        assertEquals("seller1", body.get("username"));
        assertEquals("seller@example.com", body.get("email"));
        assertEquals("1234567890", body.get("phoneNumber"));
        assertEquals("Old Shop", body.get("shopName"));
        assertEquals("Old Address", body.get("shopAddress"));
    }

    @Test
    void getSeller_AccessDenied() {
        // Try to access another user's profile
        ResponseEntity<?> response = sellerController.getSeller(2L, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updateSeller_Success() {
        Map<String, String> updates = new HashMap<>();
        updates.put("shopName", "Updated Shop");
        updates.put("shopAddress", "Updated Address");

        Seller updatedSeller = new Seller();
        updatedSeller.setUser(testUser);
        updatedSeller.setShopName("Updated Shop");
        updatedSeller.setShopAddress("Updated Address");

        when(sellerService.updateSellerProfile(1L, "Updated Shop", "Updated Address"))
                .thenReturn(updatedSeller);

        ResponseEntity<?> response = sellerController.updateSeller(1L, updates, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Seller body = (Seller) response.getBody();
        assertEquals("Updated Shop", body.getShopName());
        assertEquals("Updated Address", body.getShopAddress());
    }

    @Test
    void updateSeller_AccessDenied() {
        Map<String, String> updates = new HashMap<>();
        updates.put("shopName", "Hacked Shop");

        // Try to update another user's profile
        ResponseEntity<?> response = sellerController.updateSeller(2L, updates, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}