package com.prym.backend.integration;

import com.prym.backend.model.Seller;
import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.SellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class SellerServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private SellerService sellerService;

    // Helper: registers a seller user and returns them
    private User registerSeller(String email, String username) {
        return authService.register(
                email, "pass123", User.Role.SELLER,
                username, "Test", "Seller", "416-555-1000", null, "10001"
        );
    }

    // Test 1: createSellerProfile_PersistsToDatabase — profile is saved with correct fields
    @Test
    void createSellerProfile_PersistsToDatabase() {
        User user = registerSeller("seller_it1@example.com", "seller_it_user1");

        Seller seller = sellerService.createSellerProfile(
                user.getId(), "Integration Farm", "123 Farm Rd", "Fresh beef"
        );

        assertNotNull(seller.getId()); // ID assigned by DB confirms it was inserted
        assertEquals("Integration Farm", seller.getShopName());
        assertEquals("123 Farm Rd", seller.getShopAddress());
        assertEquals("Fresh beef", seller.getDescription());
    }

    // Test 2: createSellerProfile_DuplicateProfile_ThrowsException
    @Test
    void createSellerProfile_DuplicateProfile_ThrowsException() {
        User user = registerSeller("seller_it2@example.com", "seller_it_user2");
        sellerService.createSellerProfile(user.getId(), "Farm A", "Addr A", "Desc A");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                sellerService.createSellerProfile(user.getId(), "Farm B", "Addr B", "Desc B")
        );

        assertEquals("Seller profile already exists", ex.getMessage());
    }

    // Test 3: getSellerProfile_ReturnsCorrectProfile — retrieves the same profile that was created
    @Test
    void getSellerProfile_ReturnsCorrectProfile() {
        User user = registerSeller("seller_it3@example.com", "seller_it_user3");
        sellerService.createSellerProfile(user.getId(), "My Farm", "456 Farm Lane", "Organic beef");

        Seller fetched = sellerService.getSellerProfile(user.getId());

        assertNotNull(fetched);
        assertEquals("My Farm", fetched.getShopName());
        assertEquals("456 Farm Lane", fetched.getShopAddress());
    }

    // Test 4: updateSellerProfile_PersistsChanges — updates are saved to the DB
    @Test
    void updateSellerProfile_PersistsChanges() {
        User user = registerSeller("seller_it4@example.com", "seller_it_user4");
        sellerService.createSellerProfile(user.getId(), "Old Name", "Old Addr", "Old desc");

        Seller updated = sellerService.updateSellerProfile(
                user.getId(), "New Name", null, "New Addr", "New desc"
        );

        assertEquals("New Name", updated.getShopName());
        assertEquals("New Addr", updated.getShopAddress());
        assertEquals("New desc", updated.getDescription());
    }

    // Test 5: updateSellerProfile_WithPhoneNumber_UpdatesUser — phone number change is persisted on User
    @Test
    void updateSellerProfile_WithPhoneNumber_UpdatesUser() {
        User user = registerSeller("seller_it5@example.com", "seller_it_user5");
        sellerService.createSellerProfile(user.getId(), "Farm", "Addr", "Desc");

        sellerService.updateSellerProfile(
                user.getId(), "Farm", "647-999-1234", "Addr", "Desc"
        );

        // Re-fetch the seller profile and check the user's phone number was updated
        Seller fetched = sellerService.getSellerProfile(user.getId());
        assertEquals("647-999-1234", fetched.getUser().getPhoneNumber());
    }

    // Test 6: getAllFarms_ReturnsAtLeastOneResult — at minimum the farm we just created is returned
    @Test
    void getAllFarms_ReturnsAtLeastOneResult() {
        User user = registerSeller("seller_it6@example.com", "seller_it_user6");
        sellerService.createSellerProfile(user.getId(), "Listed Farm", "789 Road", "Listed");

        List<Seller> farms = sellerService.getAllFarms();

        assertFalse(farms.isEmpty());
        assertTrue(farms.stream().anyMatch(s -> "Listed Farm".equals(s.getShopName())));
    }

    // Test 7: getSellerProfile_NotFound_ThrowsException
    @Test
    void getSellerProfile_NotFound_ThrowsException() {
        assertThrows(RuntimeException.class, () ->
                sellerService.getSellerProfile(999999L) // ID that will never exist
        );
    }
}
