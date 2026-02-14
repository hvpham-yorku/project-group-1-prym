package com.prym.backend.controller;

import com.prym.backend.dto.SellerProfileUpdateDTO;
import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellerController sellerController;

    private User testSeller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testSeller = new User();
        testSeller.setId(1L);
        testSeller.setRole(User.Role.SELLER);
        testSeller.setUsername("seller1");
        testSeller.setFirstName("John");
        testSeller.setLastName("Doe");
        testSeller.setEmail("seller@example.com");
        testSeller.setPhoneNumber("1234567890");
    }

    @Test
    void getSeller_Success() {
        User seller = new User();
        seller.setId(1L);
        seller.setRole(User.Role.SELLER);
        seller.setUsername("seller1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));

        ResponseEntity<?> response = sellerController.getSeller(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof User);
        assertEquals("seller1", ((User) response.getBody()).getUsername());
    }

    @Test
    void getSeller_ForbiddenIfBuyer() {
        User buyer = new User();
        buyer.setId(2L);
        buyer.setRole(User.Role.BUYER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ResponseEntity<?> response = sellerController.getSeller(2L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateSeller_Username_Success() {
        Map<String, String> updates = new HashMap<>();
        updates.put("userName", "newUsername");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeller));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = sellerController.updateSeller(1L, updates);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof User);
        assertEquals("newUsername", ((User) response.getBody()).getUsername());
    }

    @Test
    void updateSeller_FirstName_Success() {
        Map<String, String> updates = new HashMap<>();
        updates.put("firstName", "Alice");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeller));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = sellerController.updateSeller(1L, updates);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof User);
        assertEquals("Alice", ((User) response.getBody()).getFirstName());
        assertEquals("seller1", ((User) response.getBody()).getUsername()); // unchanged
    }

    @Test
    void updateSeller_LastName_Success() {
        Map<String, String> updates = new HashMap<>();
        updates.put("lastName", "Smith");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeller));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = sellerController.updateSeller(1L, updates);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof User);
        assertEquals("Smith", ((User) response.getBody()).getLastName());
        assertEquals("John", ((User) response.getBody()).getFirstName()); // unchanged
    }

    @Test
    void updateSeller_PhoneNumber_Success() {
        Map<String, String> updates = new HashMap<>();
        updates.put("phoneNumber", "9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testSeller));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = sellerController.updateSeller(1L, updates);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof User);
        assertEquals("9876543210", ((User) response.getBody()).getPhoneNumber());
    }
}