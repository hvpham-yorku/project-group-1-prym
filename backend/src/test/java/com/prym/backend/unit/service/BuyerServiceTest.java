package com.prym.backend.unit.service;
import com.prym.backend.service.BuyerService;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.BuyerRepository;
import com.prym.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
        testUser.setPhoneNumber("416-555-0000");
    }

    // Test 1: Creating a profile when everything is valid
    @Test
    void createBuyerProfile_Success() {
        // Arrange: user exists and no profile exists yet
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(buyerRepository.existsByUserId(1L)).thenReturn(false);
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));

        // Act: create the profile
        Buyer result = buyerService.createBuyerProfile(1L, "Ribeye");

        // Assert: check the profile was created correctly
        assertNotNull(result);
        assertEquals("Ribeye", result.getPreferredCuts());
        verify(buyerRepository).save(any(Buyer.class));
    }

    // Test 2: Creating a profile when the user doesn't exist
    @Test
    void createBuyerProfile_UserNotFound() {
        // Arrange: user does not exist
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert: should throw an error
        assertThrows(RuntimeException.class, () -> {
            buyerService.createBuyerProfile(999L, "Ribeye");
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
            buyerService.createBuyerProfile(1L, "Ribeye");
        });
    }

    // Test 4: Getting a profile when it exists
    @Test
    void getBuyerProfile_Success() {
        // Arrange: create a buyer and tell the fake repo to return it
        Buyer buyer = new Buyer();
        buyer.setPreferredCuts("Ribeye");
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));

        // Act: get the profile
        Buyer result = buyerService.getBuyerProfile(1L);

        // Assert: check the right profile came back
        assertNotNull(result);
        assertEquals("Ribeye", result.getPreferredCuts());
    }

    // Test 5: Getting a profile that doesn't exist
    @Test
    void getBuyerProfile_NotFound() {
        // Arrange: no profile exists for this user
        when(buyerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // Act + Assert: should throw an error
        assertThrows(RuntimeException.class, () -> {
            buyerService.getBuyerProfile(999L);
        });
    }

    // Test 6: Updating a profile when it exists
    @Test
    void updateBuyerProfile_Success() {
        // Arrange: create an existing buyer with old values
        Buyer existingBuyer = new Buyer();
        existingBuyer.setPreferredCuts("Ribeye");
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(existingBuyer));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));

        // Act: update with new values
        Buyer result = buyerService.updateBuyerProfile(1L, "T-Bone", null);

        // Assert: check the fields were updated
        assertEquals("T-Bone", result.getPreferredCuts());
        verify(buyerRepository).save(any(Buyer.class));
    }

    // Test 7: Updating a profile that doesn't exist
    @Test
    void updateBuyerProfile_NotFound() {
        // Arrange: no profile exists for this user
        when(buyerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // Act + Assert: should throw an error
        assertThrows(RuntimeException.class, () -> {
            buyerService.updateBuyerProfile(999L, "Ribeye", "416-555-9999");
        });
    }

    //Test 8: Update buyer profile with phone number
    @Test
    void updateBuyerProfile_WithPhoneNumber(){
        Buyer existingBuyer = new Buyer();
        existingBuyer.setUser(testUser);
        existingBuyer.setPreferredCuts("Ribeye");
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(existingBuyer));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act: update with new values
        Buyer result = buyerService.updateBuyerProfile(1L, "T-Bone", "415-555-9999");

        // Assert: check the fields were updated
        assertEquals("T-Bone", result.getPreferredCuts());
        assertEquals("415-555-9999", existingBuyer.getUser().getPhoneNumber());
        verify(userRepository).save(any(User.class));
        verify(buyerRepository).save(any(Buyer.class));
    }

    //Test 9: Update Buyer profile with blank phone number
    @Test
    void updateBuyerProfile_WithBlankPhoneNumber(){
        Buyer existingBuyer = new Buyer();
        existingBuyer.setUser(testUser);
        existingBuyer.setPreferredCuts("Ribeye");
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(existingBuyer));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));

        buyerService.updateBuyerProfile(1L, "T-Bone", "          ");
        assertEquals("416-555-0000", existingBuyer.getUser().getPhoneNumber());
        verify(userRepository, never()).save(any(User.class));
    }

    // Test 10: getSavedFarms_Success
    @Test
    void getSavedFarms_Success() {
        Seller seller = new Seller();
        Buyer buyer = new Buyer();
        buyer.getSavedFarms().add(seller);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));

        List<Seller> result = buyerService.getSavedFarms(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(seller, result.get(0));
    }

    // Test 11: getSavedFarms_BuyerNotFound
    @Test
    void getSavedFarms_BuyerNotFound() {
        when(buyerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> buyerService.getSavedFarms(999L));
    }

    // Test 12: saveFarm_Success
    @Test
    void saveFarm_Success() {
        Seller farm = new Seller();
        Buyer buyer = new Buyer();
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));

        Buyer result = buyerService.saveFarm(1L, farm);

        assertTrue(result.getSavedFarms().contains(farm));
        verify(buyerRepository).save(buyer);
    }

    // Test 13: saveFarm_BuyerNotFound
    @Test
    void saveFarm_BuyerNotFound() {
        when(buyerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> buyerService.saveFarm(999L, new Seller()));
    }

    // Test 14: removeSavedFarm_Success
    @Test
    void removeSavedFarm_Success() {
        Seller seller = new Seller();
        seller.setId(10L);
        Buyer buyer = new Buyer();
        buyer.getSavedFarms().add(seller);
        when(buyerRepository.findByUserId(1L)).thenReturn(Optional.of(buyer));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(i -> i.getArgument(0));

        Buyer result = buyerService.removeSavedFarm(1L, 10L);

        assertTrue(result.getSavedFarms().isEmpty());
        verify(buyerRepository).save(buyer);
    }

    // Test 15: removeSavedFarm_BuyerNotFound
    @Test
    void removeSavedFarm_BuyerNotFound() {
        when(buyerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> buyerService.removeSavedFarm(999L, 10L));
    }
}