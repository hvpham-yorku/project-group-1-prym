package com.prym.backend.service;

import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.SellerRepository;
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

@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellerService sellerService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("seller@example.com");
        testUser.setRole(User.Role.SELLER);
    }

    // Test 1: Create seller profile successfully
    @Test
    void createSellerProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sellerRepository.existsByUserId(1L)).thenReturn(false);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller result = sellerService.createSellerProfile(1L, "Green Acres", "123 Farm Rd", "Organic beef farm");

        assertNotNull(result);
        assertEquals("Green Acres", result.getShopName());
        assertEquals("123 Farm Rd", result.getShopAddress());
        assertEquals("Organic beef farm", result.getDescription());
        assertEquals(testUser, result.getUser());
        verify(sellerRepository).save(any(Seller.class));
    }

    // Test 2: Create fails when user doesn't exist
    @Test
    void createSellerProfile_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                sellerService.createSellerProfile(999L, "Shop", "Address", "Desc"));

        verify(sellerRepository, never()).save(any());
    }

    // Test 3: Create fails when a profile already exists for this user
    @Test
    void createSellerProfile_DuplicateProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sellerRepository.existsByUserId(1L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                sellerService.createSellerProfile(1L, "Shop", "Address", "Desc"));

        assertEquals("Seller profile already exists", ex.getMessage());
        verify(sellerRepository, never()).save(any());
    }

    // Test 4: Get seller profile successfully
    @Test
    void getSellerProfile_Success() {
        Seller seller = new Seller();
        seller.setShopName("Green Acres");
        seller.setShopAddress("123 Farm Rd");
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(seller));

        Seller result = sellerService.getSellerProfile(1L);

        assertNotNull(result);
        assertEquals("Green Acres", result.getShopName());
        assertEquals("123 Farm Rd", result.getShopAddress());
    }

    // Test 5: Get fails when profile doesn't exist
    @Test
    void getSellerProfile_NotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                sellerService.getSellerProfile(999L));
    }

    // Test 6: Update seller profile successfully
    @Test
    void updateSellerProfile_Success() {
        Seller existingSeller = new Seller();
        existingSeller.setShopName("Old Shop");
        existingSeller.setShopAddress("Old Address");
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller result = sellerService.updateSellerProfile(1L, "New Shop", "New Address", "New Desc", null);

        assertEquals("New Shop", result.getShopName());
        assertEquals("New Address", result.getShopAddress());
        assertEquals("New Desc", result.getDescription());
        verify(sellerRepository).save(any(Seller.class));
    }

    // Test 7: Update fails when profile doesn't exist
    @Test
    void updateSellerProfile_NotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                sellerService.updateSellerProfile(999L, "Shop", "Addr", "Desc", null));
    }

    // Test 8: Update sets a valid category
    @Test
    void updateSellerProfile_WithCategory() {
        Seller existingSeller = new Seller();
        existingSeller.setUser(testUser);
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller result = sellerService.updateSellerProfile(1L, "Shop", "Addr", "Desc", "HALAL");

        assertEquals(Seller.SellerCategory.HALAL, result.getCategory());
    }

    // Test 9: Update with an invalid category string throws an exception
    @Test
    void updateSellerProfile_InvalidCategory() {
        Seller existingSeller = new Seller();
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingSeller));

        assertThrows(IllegalArgumentException.class, () ->
                sellerService.updateSellerProfile(1L, "Shop", "Addr", "Desc", "NOTACATEGORY"));
    }
}
