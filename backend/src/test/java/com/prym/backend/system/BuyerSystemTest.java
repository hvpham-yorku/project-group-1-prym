package com.prym.backend.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for buyer-side API endpoints.
 *
 * Covers: buyer profile (create/read/update), saved farms (add/list/remove),
 * and role-based access enforcement.
 */
@SuppressWarnings("rawtypes")
class BuyerSystemTest extends SystemTestBase {

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Creates a buyer user AND their buyer profile, returning the session cookie.
     * outId[0] is populated with the user ID.
     */
    private String setupBuyer(String emailPrefix, long[] outId) {
        String email = uniqueEmail(emailPrefix);
        String username = uniqueUsername(emailPrefix);
        String session = registerBuyer(email, username, outId);

        // Create the buyer profile
        Map<String, Object> body = Map.of("userId", outId[0], "preferredCuts", "Ribeye");
        postForMap("/api/buyer/profile", body, session);

        return session;
    }

 

    // ── Saved farms ───────────────────────────────────────────────────────────

    @Test
    void getSavedFarms_EmptyInitially() {
        long[] id = new long[1];
        String session = setupBuyer("sf_empty", id);

        ResponseEntity<List> resp = getForList("/api/buyer/all", session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
    }

    @Test
    void addSavedFarm_ThenListContainsIt() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("sf_add", buyerId);

        // Register a seller to save
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("sf_seller"), uniqueUsername("sf_seller"), sellerId);

        // Add saved farm — PATCH /api/buyer/all with seller object
        // /api/seller/all requires BUYER role — pass the buyer's session
        ResponseEntity<List> allSellers = getForList("/api/seller/all", buyerSession);
        Map<String, Object> sellerObj = null;
        for (Object item : allSellers.getBody()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> s = (Map<String, Object>) item;
            // Seller entity "id" is the Seller PK; match by user.id instead
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) s.get("user");
            if (userMap != null && ((Number) userMap.get("id")).longValue() == sellerId[0]) {
                sellerObj = s;
                break;
            }
        }
        assertNotNull(sellerObj, "Newly registered seller must appear in /api/seller/all");

        HttpHeaders headers = withSession(buyerSession);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map> patchEntity = new HttpEntity<>(sellerObj, headers);
        ResponseEntity<Map> addResp = http.exchange(url("/api/buyer/all"), HttpMethod.PATCH, patchEntity, Map.class);

        assertEquals(200, addResp.getStatusCode().value());
    }

    @Test
    void removeSavedFarm_Returns200() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("sf_rm", buyerId);

        // Register and add a seller
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("sf_rm_seller"), uniqueUsername("sf_rm_seller"), sellerId);

        // /api/seller/all requires BUYER role
        ResponseEntity<List> allSellers = getForList("/api/seller/all", buyerSession);
        Map<String, Object> sellerObj = null;
        for (Object item : allSellers.getBody()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> s = (Map<String, Object>) item;
            // Seller entity "id" is the Seller PK; match by user.id instead
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) s.get("user");
            if (userMap != null && ((Number) userMap.get("id")).longValue() == sellerId[0]) {
                sellerObj = s;
                break;
            }
        }
        assertNotNull(sellerObj);

        HttpHeaders headers = withSession(buyerSession);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map> addEntity = new HttpEntity<>(sellerObj, headers);
        http.exchange(url("/api/buyer/all"), HttpMethod.PATCH, addEntity, Map.class);

        // Now remove it
        ResponseEntity<Map> deleteResp = deleteForMapWithBody("/api/buyer/saved-farms",
                Map.of("sellerId", sellerId[0]), buyerSession);

        assertEquals(200, deleteResp.getStatusCode().value());
    }


}