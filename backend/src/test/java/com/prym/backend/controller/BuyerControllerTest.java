package com.prym.backend.controller;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.BuyerService;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BuyerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuyerService buyerService;

    // BuyerController uses UserRepository directly to resolve the logged-in user
    @MockitoBean
    private UserRepository userRepository;

    // SessionFilter is a Spring component that requires SessionService — must be mocked even though BuyerController doesn't use it directly
    @MockitoBean
    private SessionService sessionService;

    private User testUser;
    private Buyer testBuyer;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRole(User.Role.BUYER);

        testBuyer = new Buyer();
        testBuyer.setUser(testUser);
        testBuyer.setPreferredCuts("Ribeye");
        testBuyer.setQuantity("Half cow");

        // Simulate a logged-in user named "buyer@example.com" in the security context
        // BuyerController calls SecurityContextHolder.getContext().getAuthentication().getName()
        // to get the email, then looks it up in the UserRepository
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("buyer@example.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext(); // prevent security context leaking between tests
    }

    // Test 1: Create buyer profile successfully
    @Test
    void createProfile_Success() throws Exception {
        when(buyerService.createBuyerProfile(1L, "Ribeye", "Half cow")).thenReturn(testBuyer);

        mockMvc.perform(post("/api/buyer/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"1\", \"preferredCuts\": \"Ribeye\", \"quantity\": \"Half cow\"}"))
                .andExpect(status().isOk());
    }

    // Test 2: Create profile is rejected when userId doesn't match the logged-in user
    @Test
    void createProfile_AccessDenied() throws Exception {
        // userId 99 doesn't match the logged-in user's id (1)
        mockMvc.perform(post("/api/buyer/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"99\", \"preferredCuts\": \"Ribeye\", \"quantity\": \"Half cow\"}"))
                .andExpect(status().isForbidden());
    }

    // Test 3: Get buyer profile successfully
    @Test
    void getProfile_Success() throws Exception {
        when(buyerService.getBuyerProfile(1L)).thenReturn(testBuyer);

        mockMvc.perform(get("/api/buyer/profile/1"))
                .andExpect(status().isOk());
    }

    // Test 4: Get profile is rejected when the path userId doesn't match the logged-in user
    @Test
    void getProfile_AccessDenied() throws Exception {
        mockMvc.perform(get("/api/buyer/profile/99"))
                .andExpect(status().isForbidden());
    }

    // Test 5: Update buyer profile successfully
    @Test
    void updateProfile_Success() throws Exception {
        Buyer updatedBuyer = new Buyer();
        updatedBuyer.setUser(testUser);
        updatedBuyer.setPreferredCuts("T-Bone");
        updatedBuyer.setQuantity("Whole cow");
        when(buyerService.updateBuyerProfile(1L, "T-Bone", "Whole cow")).thenReturn(updatedBuyer);

        mockMvc.perform(patch("/api/buyer/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredCuts\": \"T-Bone\", \"quantity\": \"Whole cow\"}"))
                .andExpect(status().isOk());
    }

    // Test 6: Update profile is rejected when the path userId doesn't match the logged-in user
    @Test
    void updateProfile_AccessDenied() throws Exception {
        mockMvc.perform(patch("/api/buyer/profile/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredCuts\": \"T-Bone\", \"quantity\": \"Whole cow\"}"))
                .andExpect(status().isForbidden());
    }

    // Test 7: Update returns 400 when the buyer profile does not exist
    @Test
    void updateProfile_NotFound() throws Exception {
        when(buyerService.updateBuyerProfile(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("Buyer profile not found"));

        mockMvc.perform(patch("/api/buyer/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredCuts\": \"T-Bone\", \"quantity\": \"Whole cow\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Buyer profile not found"));
    }
}
