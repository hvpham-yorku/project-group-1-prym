package com.prym.backend.service;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.User;
import com.prym.backend.repository.BuyerRepository;
import com.prym.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Uses Mockito to create fake repositories so we can test the service without a real database
@ExtendWith(MockitoExtension.class)
public class BuyerServiceTest {

    @Mock // fake BuyerRepository
    private BuyerRepository buyerRepository;

    @Mock // fake UserRepository
    private UserRepository userRepository;

    @InjectMocks // real BuyerService but with the fakes injected
    private BuyerService buyerService;

    private User testUser; // reusable test data

    // Runs before every test to set up a fresh test user
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole(User.Role.BUYER);
    }

    // Test 1: Creating a profile when everything is valid
    @Test
    void createBuyerProfile_Success() {
        // Arrange: user exists and no profile exists yet
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(buyerRepository.existsByUserId(1L)).thenReturn(false);
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create the profile
        Buyer result = buyerService.createBuyerProfile(1L, "Shayan", "Darajeh",
                "123-456-7890", "Ribeye", "Half cow");

        // Assert: check the profile was created correctly
        assertNotNull(result);
        assertEquals("Shayan", result.getFirstName());
        assertEquals("Darajeh", result.getLastName());
        assertEquals("123-456-7890", result.getPhoneNumber());
        assertEquals("Ribeye", result.getPreferredCuts());
        assertEquals("Half cow", result.getQuantity());
        verify(buyerRepository).save(any(Buyer.class)); // verify save was actually called
    }

    // Test 2: Creating a profile when the user doesn't exist
    @Test
    void createBuyerProfile_UserNotFound() {
        // Arrange: user does not exist
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert: should throw an error
        assertThrows(RuntimeException.class, () -> {
            buyerService.createBuyerProfile(999L, "Shayan", "Darajeh",
                    "123-456-7890", "Ribeye", "Half cow");
        });
    }

    // Test 3: Creating a profile when one already exists for this user
    @Test
    void createBuyerProfile_DuplicateProfile() {
        // Arrange: user exists but already has a profile
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(buyerRepository.existsByUserId(1L)).thenReturn(true);

        // Act + Assert: should throw an error
        assertThrows(RuntimeException.class, () -> {
            buyerService.createBuyerProfile(1L, "Shayan", "Darajeh",
                    "123-456-7890", "Ribeye", "Half cow");
        });
    }
}
