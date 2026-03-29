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


}