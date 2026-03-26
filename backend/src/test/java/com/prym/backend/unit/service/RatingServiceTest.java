package com.prym.backend.unit.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import com.prym.backend.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private RatingCodeRepository ratingCodeRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private BuyerRepository buyerRepository;

    @InjectMocks
    private RatingService ratingService;

    private User sellerUser;
    private User buyerUser;
    private Seller seller;
    private Buyer buyer;
    private RatingCode ratingCode;

    @BeforeEach
    void setUp() {
        sellerUser = new User();
        sellerUser.setId(1L);
        sellerUser.setUsername("farmuser");
        sellerUser.setRole(User.Role.SELLER);

        seller = new Seller();
        seller.setId(10L);
        seller.setUser(sellerUser);
        seller.setShopName("Green Farm");
        seller.setAverageRating(0.0);
        seller.setTotalRatings(0);

        buyerUser = new User();
        buyerUser.setId(2L);
        buyerUser.setRole(User.Role.BUYER);

        buyer = new Buyer();
        buyer.setId(20L);
        buyer.setUser(buyerUser);

        ratingCode = new RatingCode();
        ratingCode.setCode("PRYM-ABC123");
        ratingCode.setSeller(seller);
        ratingCode.setUsed(false);
    }

    // Test 1: submitRating — happy path updates averageRating and totalRatings
    @Test
    void submitRating_Success_UpdatesSellerAggregates() {
        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer));
        when(ratingCodeRepository.markAsUsed("PRYM-ABC123")).thenReturn(1);
        when(ratingCodeRepository.findByCode("PRYM-ABC123")).thenReturn(Optional.of(ratingCode));
        when(sellerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seller));
        when(ratingRepository.existsBySellerAndBuyer(seller, buyer)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(i -> i.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = ratingService.submitRating(2L, "PRYM-ABC123", 4);

        assertEquals("Rating submitted successfully!", result.get("message"));

        ArgumentCaptor<Seller> sellerCaptor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository).save(sellerCaptor.capture());
        Seller saved = sellerCaptor.getValue();
        assertEquals(1, saved.getTotalRatings());
        assertEquals(4.0, saved.getAverageRating(), 0.001);
    }

    // Test 2: submitRating — average is computed correctly from prior ratings
    @Test
    void submitRating_AverageCalculation_IsCorrect() {
        seller.setTotalRatings(2);
        seller.setAverageRating(3.0); // existing average of 2 ratings

        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer));
        when(ratingCodeRepository.markAsUsed("PRYM-ABC123")).thenReturn(1);
        when(ratingCodeRepository.findByCode("PRYM-ABC123")).thenReturn(Optional.of(ratingCode));
        when(sellerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seller));
        when(ratingRepository.existsBySellerAndBuyer(seller, buyer)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(i -> i.getArgument(0));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        ratingService.submitRating(2L, "PRYM-ABC123", 5);

        // (3.0 * 2 + 5) / 3 = 11/3 ≈ 3.667
        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getTotalRatings());
        assertEquals(11.0 / 3.0, captor.getValue().getAverageRating(), 0.001);
    }

    // Test 3: submitRating — invalid score throws
    @Test
    void submitRating_InvalidScore_Throws() {
        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(2L, "PRYM-ABC123", 6));
        assertEquals("Score must be between 1 and 5.", ex.getMessage());
    }

    // Test 4: submitRating — already-used code throws
    @Test
    void submitRating_UsedCode_Throws() {
        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer));
        when(ratingCodeRepository.markAsUsed("PRYM-ABC123")).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(2L, "PRYM-ABC123", 4));
        assertEquals("Invalid or already-used code.", ex.getMessage());
    }

    // Test 5: submitRating — duplicate rating throws
    @Test
    void submitRating_DuplicateRating_Throws() {
        when(buyerRepository.findByUserId(2L)).thenReturn(Optional.of(buyer));
        when(ratingCodeRepository.markAsUsed("PRYM-ABC123")).thenReturn(1);
        when(ratingCodeRepository.findByCode("PRYM-ABC123")).thenReturn(Optional.of(ratingCode));
        when(sellerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seller));
        when(ratingRepository.existsBySellerAndBuyer(seller, buyer)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(2L, "PRYM-ABC123", 4));
        assertEquals("You have already rated this seller.", ex.getMessage());
    }

    // Test 6: getFarmRatings — returns cached averageRating and totalRatings from seller
    @Test
    void getFarmRatings_ReturnsCachedAggregates() {
        seller.setAverageRating(4.2);
        seller.setTotalRatings(5);
        when(sellerRepository.findByUserUsername("farmuser")).thenReturn(Optional.of(seller));
        when(ratingRepository.findBySeller(seller)).thenReturn(List.of());

        Map<String, Object> result = ratingService.getFarmRatings("farmuser");

        assertEquals(4.2, (double) result.get("averageRating"), 0.001);
        assertEquals(5, result.get("totalRatings"));
        assertEquals("Green Farm", result.get("shopName"));
    }

    // Test 7: generateRatingCode — creates a code and persists it
    @Test
    void generateRatingCode_Success_ReturnsCode() {
        when(sellerRepository.findByUserId(1L)).thenReturn(Optional.of(seller));
        when(ratingCodeRepository.save(any(RatingCode.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = ratingService.generateRatingCode(1L);

        assertNotNull(result.get("code"));
        assertTrue(result.get("code").toString().startsWith("PRYM-"));
        verify(ratingCodeRepository).save(any(RatingCode.class));
    }
}
