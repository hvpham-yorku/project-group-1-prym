package com.prym.backend.unit.controller;
import com.prym.backend.controller.BuyerController;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.BuyerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BuyerControllerTest {

    @Mock
    private BuyerService buyerService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BuyerController buyerController;

    private User testUser;
    private Buyer testBuyer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRole(User.Role.BUYER);

        testBuyer = new Buyer();
        testBuyer.setUser(testUser);
        testBuyer.setPreferredCuts("Chuck, Rib x2");

        // Simulate logged-in user via Spring Security
        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "buyer@example.com", null);
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);
        // When controller looks up the logged-in user, return testUser
        when(userRepository.findByEmail("buyer@example.com"))
                .thenReturn(Optional.of(testUser));
    }

    // Test 1: createProfile_Success
    @Test
    public void createProfile_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "1");
        request.put("preferredCuts", "Chuck, Rib x2");

        when(buyerService.createBuyerProfile(1L, "Chuck, Rib x2"))
                .thenReturn(testBuyer);

        ResponseEntity<?> response = buyerController.createProfile(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testBuyer, response.getBody());
    }

    // Test 2: createProfile_ForbiddenForOtherUser
    @Test
    public void createProfile_ForbiddenForOtherUser() {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "2"); // different from logged-in user ID 1
        request.put("preferredCuts", "Chuck");

        ResponseEntity<?> response = buyerController.createProfile(request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 3: createProfile_AlreadyExists
    @Test
    public void createProfile_AlreadyExists() {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "1");
        request.put("preferredCuts", "Chuck");

        when(buyerService.createBuyerProfile(1L, "Chuck"))
                .thenThrow(new RuntimeException("Buyer profile already exists"));

        ResponseEntity<?> response = buyerController.createProfile(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Buyer profile already exists", body.get("error"));
    }

    // Test 4: getProfile_Success
    @Test
    public void getProfile_Success() {
        when(buyerService.getBuyerProfile(1L)).thenReturn(testBuyer);

        ResponseEntity<?> response = buyerController.getProfile(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testBuyer, response.getBody());
    }

    // Test 5: getProfile_ForbiddenForOtherUser
    @Test
    public void getProfile_ForbiddenForOtherUser() {
        ResponseEntity<?> response = buyerController.getProfile(2L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 6: getProfile_NotFound
    @Test
    public void getProfile_NotFound() {
        when(buyerService.getBuyerProfile(1L))
                .thenThrow(new RuntimeException("Buyer profile not found"));

        ResponseEntity<?> response = buyerController.getProfile(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Buyer profile not found", body.get("error"));
    }

    // Test 7: updateProfile_Success
    @Test
    public void updateProfile_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("preferredCuts", "Chuck, Rib x2");
        request.put("phoneNumber", "416-555-9999");

        when(buyerService.updateBuyerProfile(1L, "Chuck, Rib x2", "416-555-9999"))
                .thenReturn(testBuyer);

        ResponseEntity<?> response = buyerController.updateProfile(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testBuyer, response.getBody());
    }

    // Test 8: updateProfile_ForbiddenForOtherUser
    @Test
    public void updateProfile_ForbiddenForOtherUser() {
        Map<String, String> request = new HashMap<>();
        request.put("preferredCuts", "Chuck");

        ResponseEntity<?> response = buyerController.updateProfile(2L, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 9: updateProfile_ServiceError
    @Test
    public void updateProfile_ServiceError() {
        Map<String, String> request = new HashMap<>();
        request.put("preferredCuts", "Chuck");
        request.put("phoneNumber", null);

        when(buyerService.updateBuyerProfile(1L, "Chuck", null))
                .thenThrow(new RuntimeException("Buyer profile not found"));

        ResponseEntity<?> response = buyerController.updateProfile(1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Buyer profile not found", body.get("error"));
    }

    // Test 10: getSavedFarms_Success
    @Test
    public void getSavedFarms_Success() {
        Seller seller = new Seller();
        List<Seller> savedFarms = List.of(seller);
        when(buyerService.getSavedFarms(1L)).thenReturn(savedFarms);

        ResponseEntity<?> response = buyerController.getSavedFarms();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(savedFarms, response.getBody());
    }

    // Test 11: getSavedFarms_ServiceError
    @Test
    public void getSavedFarms_ServiceError() {
        when(buyerService.getSavedFarms(1L))
                .thenThrow(new RuntimeException("Buyer profile not found"));

        ResponseEntity<?> response = buyerController.getSavedFarms();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // Test 12: saveFarm_Success
    @Test
    public void saveFarm_Success() {
        Seller farm = new Seller();
        Buyer buyerWithFarm = new Buyer();
        buyerWithFarm.setUser(testUser);
        // testBuyer has an empty savedFarms list so the farm is not yet saved
        when(buyerService.getBuyerProfile(1L)).thenReturn(testBuyer);
        when(buyerService.saveFarm(1L, farm)).thenReturn(buyerWithFarm);

        ResponseEntity<?> response = buyerController.saveFarm(farm);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(buyerService).saveFarm(1L, farm);
    }

    // Test 13: saveFarm_AlreadySaved_SkipsSave
    @Test
    public void saveFarm_AlreadySaved_SkipsSave() {
        Seller farm = new Seller();
        testBuyer.getSavedFarms().add(farm);
        when(buyerService.getBuyerProfile(1L)).thenReturn(testBuyer);

        ResponseEntity<?> response = buyerController.saveFarm(farm);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(buyerService, never()).saveFarm(anyLong(), any(Seller.class));
    }

    // Test 14: saveFarm_ServiceError
    @Test
    public void saveFarm_ServiceError() {
        Seller farm = new Seller();
        when(buyerService.getBuyerProfile(1L))
                .thenThrow(new RuntimeException("Buyer profile not found"));

        ResponseEntity<?> response = buyerController.saveFarm(farm);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // Test 15: removeSavedFarm_Success
    @Test
    public void removeSavedFarm_Success() {
        Map<String, Long> body = new HashMap<>();
        body.put("sellerId", 10L);
        when(buyerService.removeSavedFarm(1L, 10L)).thenReturn(testBuyer);

        ResponseEntity<?> response = buyerController.removeSavedFarm(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(buyerService).removeSavedFarm(1L, 10L);
    }

    // Test 16: removeSavedFarm_MissingSellerId
    @Test
    public void removeSavedFarm_MissingSellerId() {
        Map<String, Long> body = new HashMap<>();

        ResponseEntity<?> response = buyerController.removeSavedFarm(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> respBody = (Map<?, ?>) response.getBody();
        assertEquals("sellerId is required", respBody.get("error"));
    }

    // Test 17: removeSavedFarm_ServiceError
    @Test
    public void removeSavedFarm_ServiceError() {
        Map<String, Long> body = new HashMap<>();
        body.put("sellerId", 10L);
        when(buyerService.removeSavedFarm(1L, 10L))
                .thenThrow(new RuntimeException("Buyer profile not found"));

        ResponseEntity<?> response = buyerController.removeSavedFarm(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
