package com.prym.backend.service;

import com.prym.backend.model.CowType;
import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.repository.CowTypeRepository;
import com.prym.backend.repository.SellerRepository;
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

@ExtendWith(MockitoExtension.class)
public class CowTypeServiceTest {

    @Mock
    private CowTypeRepository cowTypeRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private CowTypeService cowTypeService;

    private Seller testSeller;
    private CowType testCowType;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(User.Role.SELLER);

        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setUser(testUser);
        testSeller.setShopName("Test Farm");

        testCowType = new CowType();
        testCowType.setId(1L);
        testCowType.setSeller(testSeller);
        testCowType.setBreed(CowType.Breed.ANGUS);
        testCowType.setDescription("Grass-fed Angus");
        testCowType.setPricePerPound(5.99);
        testCowType.setAvailableCount(3);
    }

    // Test 1: createCowType_Success
    @Test
    void createCowType_Success() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));
        when(cowTypeRepository.save(any(CowType.class))).thenAnswer(i -> i.getArgument(0));

        CowType result = cowTypeService.createCowType(1L, "ANGUS", "Grass-fed Angus", 5.99, 3);

        assertNotNull(result);
        assertEquals(CowType.Breed.ANGUS, result.getBreed());
        assertEquals("Grass-fed Angus", result.getDescription());
        assertEquals(5.99, result.getPricePerPound());
        assertEquals(3, result.getAvailableCount());
        verify(cowTypeRepository).save(any(CowType.class));
    }

    // Test 2: createCowType_SellerNotFound
    @Test
    void createCowType_SellerNotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> cowTypeService.createCowType(999L, "ANGUS", "desc", 5.99, 1));
        verify(cowTypeRepository, never()).save(any());
    }

    // Test 3: createCowType_InvalidBreed
    @Test
    void createCowType_InvalidBreed() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));

        assertThrows(IllegalArgumentException.class,
                () -> cowTypeService.createCowType(1L, "NONEXISTENT_BREED", "desc", 5.99, 1));
    }

    // Test 4: getCowTypesBySeller_Success
    @Test
    void getCowTypesBySeller_Success() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(testSeller));
        when(cowTypeRepository.findBySellerId(1L)).thenReturn(List.of(testCowType));

        List<CowType> result = cowTypeService.getCowTypesBySeller(1L);

        assertEquals(1, result.size());
        assertEquals(CowType.Breed.ANGUS, result.get(0).getBreed());
    }

    // Test 5: getCowTypesBySeller_SellerNotFound
    @Test
    void getCowTypesBySeller_SellerNotFound() {
        when(sellerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> cowTypeService.getCowTypesBySeller(999L));
    }

    // Test 6: updateCowType_Success
    @Test
    void updateCowType_Success() {
        when(cowTypeRepository.findById(1L)).thenReturn(Optional.of(testCowType));
        when(cowTypeRepository.save(any(CowType.class))).thenAnswer(i -> i.getArgument(0));

        CowType result = cowTypeService.updateCowType(1L, "WAGYU", "Premium Wagyu", 12.99, 2);

        assertEquals(CowType.Breed.WAGYU, result.getBreed());
        assertEquals("Premium Wagyu", result.getDescription());
        assertEquals(12.99, result.getPricePerPound());
        assertEquals(2, result.getAvailableCount());
        verify(cowTypeRepository).save(any(CowType.class));
    }

    // Test 7: updateCowType_NotFound
    @Test
    void updateCowType_NotFound() {
        when(cowTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> cowTypeService.updateCowType(999L, "ANGUS", null, null, null));
    }

    // Test 8: updateCowType_NullFieldsSkipped
    @Test
    void updateCowType_NullFieldsSkipped() {
        when(cowTypeRepository.findById(1L)).thenReturn(Optional.of(testCowType));
        when(cowTypeRepository.save(any(CowType.class))).thenAnswer(i -> i.getArgument(0));

        // Only update description, leave breed/price/count unchanged
        CowType result = cowTypeService.updateCowType(1L, null, "Updated desc", null, null);

        assertEquals(CowType.Breed.ANGUS, result.getBreed()); // unchanged
        assertEquals("Updated desc", result.getDescription()); // updated
        assertEquals(5.99, result.getPricePerPound()); // unchanged
    }

    // Test 9: deleteCowType_Success
    @Test
    void deleteCowType_Success() {
        cowTypeService.deleteCowType(1L);
        verify(cowTypeRepository).deleteById(1L);
    }
}
