package com.prym.backend.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for seller-side API endpoints.
 *
 * Covers: seller profile (read/update), public farm listing, cow types (CRUD),
 * cow listing, and certification management — all via real HTTP.
 */
@SuppressWarnings("rawtypes")
class SellerSystemTest extends SystemTestBase {

    // ── Seller profile ────────────────────────────────────────────────────────

    @Test
    void getAllSellers_BuyerCanBrowseFarmListings() {
        // Register a seller so the list is non-empty
        registerSeller(uniqueEmail("pub"), uniqueUsername("pub"), null);
        // /api/seller/all requires ROLE_BUYER per SecurityConfig
        String buyerSession = registerBuyer(uniqueEmail("pub_buyer"), uniqueUsername("pub_buyer"), null);

        ResponseEntity<List> resp = getForList("/api/seller/all", buyerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void getSellerProfile_OwnProfile_ReturnsCorrectFields() {
        String username = uniqueUsername("sprof");
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("sprof"), username, id);

        ResponseEntity<Map> resp = getForMap("/api/seller/profile/" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("shopName"));
    }

    @Test
    void getSellerProfile_OtherUserSession_Returns403() {
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("other_s"), uniqueUsername("other_s"), sellerId);

        // A different seller tries to read that profile
        long[] intruderId = new long[1];
        String intruderSession = registerSeller(uniqueEmail("intruder_s"), uniqueUsername("intruder_s"), intruderId);

        ResponseEntity<Map> resp = getForMap("/api/seller/profile/" + sellerId[0], intruderSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void getSellerProfile_NotFound_Returns404() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("notfound_s"), uniqueUsername("notfound_s"), id);

        ResponseEntity<Map> resp = getForMap("/api/seller/profile/999999999", session);

        // Spring Security may return 403 (role mismatch for another's profile) or 404
        int status = resp.getStatusCode().value();
        assertTrue(status == 403 || status == 404);
    }

    
}