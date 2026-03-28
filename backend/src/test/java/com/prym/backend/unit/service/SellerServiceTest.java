package com.prym.backend.unit.service;
import com.prym.backend.service.SellerService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.SellerRepository;
import com.prym.backend.repository.UserRepository;

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
        testUser.setPhoneNumber("416-555-0000");
        testUser.setRole(User.Role.SELLER);
    }

    // Test 1: createSellerProfile_Success
    @Test
    public void createSellerProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sellerRepository.existsByUserId(1L)).thenReturn(false);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller result = sellerService.createSellerProfile(1L, "John's Meats", "123 Main St", "Premium Cuts");

        assertNotNull(result);
        assertEquals("John's Meats", result.getShopName());
        assertEquals("123 Main St", result.getShopAddress());
        assertEquals("Premium Cuts", result.getDescription());
        verify(sellerRepository).save(any(Seller.class));
    }

    // Test 2: createSellerProfile_UserNotFound
    @Test
    public void createSellerProfile_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sellerService.createSellerProfile(999L, "Shop", "Address", "Desc"));
    }

    // Test 3: createSellerProfile_DuplicateProfile
    @Test
    public void createSellerProfile_DuplicateProfile() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(sellerRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> sellerService.createSellerProfile(1L, "Shop", "Address", "Desc"));
    }

    // Test 4: getSellerProfile_Success
    @Test
    public void getSellerProfile_Success() {
        Seller seller = new Seller();
        seller.setUser(testUser);
        seller.setShopName("John's Meats");
        seller.setShopAddress("123 Main St");
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(seller));

        Seller result = sellerService.getSellerProfile(1L);

        // check the profile
        assertNotNull(result);
        assertEquals("John's Meats", result.getShopName());
        assertEquals("123 Main St", result.getShopAddress());
    }

    // Test 5: getSellerProfile_NotFound
    @Test
    public void getSellerProfile_NotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sellerService.getSellerProfile(999L));
    }

    // Test 6: updateSellerProfile_Success
    @Test
    public void updateSellerProfile_Success() {
        Seller existingSeller = new Seller();
        existingSeller.setUser(testUser);
        existingSeller.setShopName("Old Shop");
        existingSeller.setShopAddress("Old Address");
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller result = sellerService.updateSellerProfile(1L, "New Shop", null, "New Address", "Great beef");

        assertEquals("New Shop", result.getShopName());
        assertEquals("New Address", result.getShopAddress());
        assertEquals("Great beef", result.getDescription());

        verify(sellerRepository).save(any(Seller.class));
    }

    // Test 7: updateSellerProfile_WithPhoneNumber
    @Test
    public void updateSellerProfile_WithPhoneNumber() {
        Seller existingSeller = new Seller();
        existingSeller.setUser(testUser);
        existingSeller.setShopName("My Shop");
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        sellerService.updateSellerProfile(1L, "My Shop", "647-555-1234", "Same Address", null);

        assertEquals("647-555-1234", existingSeller.getUser().getPhoneNumber());
        verify(userRepository).save(any(User.class));
    }

    // Test 8: updateSellerProfile_BlankPhoneSkipped
    @Test
    public void updateSellerProfile_BlankPhoneSkipped() {
        Seller existingSeller = new Seller();
        existingSeller.setUser(testUser);
        existingSeller.setShopName("My Shop");
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        sellerService.updateSellerProfile(1L, "My Shop", "   ", "Address", null);

        assertEquals("416-555-0000", existingSeller.getUser().getPhoneNumber());
        verify(userRepository, never()).save(any(User.class));
    }

    // Test 9: updateSellerProfile_NotFound
    @Test
    public void updateSellerProfile_NotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> sellerService.updateSellerProfile(999L, "Shop", null, "Addr", null));
    }

    // Test 10: getAllFarms_ReturnsAllSellers
    @Test
    public void getAllFarms_ReturnsAllSellers() {
        Seller s1 = new Seller();
        s1.setUser(testUser);
        s1.setShopName("Farm A");

        Seller s2 = new Seller();
        User user2 = new User();
        user2.setId(2L);
        s2.setUser(user2);
        s2.setShopName("Farm B");

        when(sellerRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Seller> result = sellerService.getAllFarms();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Farm A", result.get(0).getShopName());
        assertEquals("Farm B", result.get(1).getShopName());
    }

    // Test 11: getAllFarms_ReturnsEmptyListWhenNoSellers
    @Test
    public void getAllFarms_ReturnsEmptyListWhenNoSellers() {
        when(sellerRepository.findAll()).thenReturn(List.of());

        List<Seller> result = sellerService.getAllFarms();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // Test 12: setRating_Success
    @Test
    public void setRating_Success() {
        Seller seller = new Seller();
        seller.setUser(testUser);
        seller.setAverageRating(0.0);
        seller.setTotalRatings(0);
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller result = sellerService.setRating(1L, 4.5, 10);

        assertEquals(4.5, result.getAverageRating());
        assertEquals(10, result.getTotalRatings());
        verify(sellerRepository).save(seller);
    }

    // Test 13: setRating_SellerNotFound
    @Test
    public void setRating_SellerNotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sellerService.setRating(999L, 4.5, 10));
    }
}
