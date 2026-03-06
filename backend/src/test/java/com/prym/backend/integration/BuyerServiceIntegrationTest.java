package com.prym.backend.integration;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.BuyerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class BuyerServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private BuyerService buyerService;

    // Helper: registers a buyer user and returns them
    private User registerBuyer(String email, String username) {
        return authService.register(
                email, "pass123", User.Role.BUYER,
                username, "Test", "Buyer", "416-555-2000", null
        );
    }

    // Test 1: createBuyerProfile_PersistsToDatabase — buyer profile is saved with correct fields
    @Test
    void createBuyerProfile_PersistsToDatabase() {
        User user = registerBuyer("buyer_it1@example.com", "buyer_it_user1");

        Buyer buyer = buyerService.createBuyerProfile(
                user.getId(), "Ribeye, Chuck", "Half cow"
        );

        assertNotNull(buyer.getId()); // ID assigned by DB confirms it was inserted
        assertEquals("Ribeye, Chuck", buyer.getPreferredCuts());
        assertEquals("Half cow", buyer.getQuantity());
    }

    // Test 2: createBuyerProfile_DuplicateProfile_ThrowsException
    @Test
    void createBuyerProfile_DuplicateProfile_ThrowsException() {
        User user = registerBuyer("buyer_it2@example.com", "buyer_it_user2");
        buyerService.createBuyerProfile(user.getId(), "Chuck", "Quarter cow");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                buyerService.createBuyerProfile(user.getId(), "Rib", "Half cow")
        );

        assertEquals("Buyer profile already exists", ex.getMessage());
    }

    // Test 3: getBuyerProfile_ReturnsCorrectProfile — retrieves the same profile that was created
    @Test
    void getBuyerProfile_ReturnsCorrectProfile() {
        User user = registerBuyer("buyer_it3@example.com", "buyer_it_user3");
        buyerService.createBuyerProfile(user.getId(), "Brisket", "Whole cow");

        Buyer fetched = buyerService.getBuyerProfile(user.getId());

        assertNotNull(fetched);
        assertEquals("Brisket", fetched.getPreferredCuts());
        assertEquals("Whole cow", fetched.getQuantity());
    }

    // Test 4: updateBuyerProfile_PersistsChanges — updates are saved to the DB
    @Test
    void updateBuyerProfile_PersistsChanges() {
        User user = registerBuyer("buyer_it4@example.com", "buyer_it_user4");
        buyerService.createBuyerProfile(user.getId(), "Old cuts", "Quarter cow");

        Buyer updated = buyerService.updateBuyerProfile(
                user.getId(), "New cuts", "Half cow", null
        );

        assertEquals("New cuts", updated.getPreferredCuts());
        assertEquals("Half cow", updated.getQuantity());
    }

    // Test 5: updateBuyerProfile_WithPhoneNumber_UpdatesUser — phone number change is persisted on User
    @Test
    void updateBuyerProfile_WithPhoneNumber_UpdatesUser() {
        User user = registerBuyer("buyer_it5@example.com", "buyer_it_user5");
        buyerService.createBuyerProfile(user.getId(), "Chuck", "Quarter cow");

        buyerService.updateBuyerProfile(user.getId(), "Chuck", "Quarter cow", "647-888-5555");

        Buyer fetched = buyerService.getBuyerProfile(user.getId());
        assertEquals("647-888-5555", fetched.getUser().getPhoneNumber());
    }

    // Test 6: getBuyerProfile_NotFound_ThrowsException
    @Test
    void getBuyerProfile_NotFound_ThrowsException() {
        assertThrows(RuntimeException.class, () ->
                buyerService.getBuyerProfile(999999L) // ID that will never exist
        );
    }
}
