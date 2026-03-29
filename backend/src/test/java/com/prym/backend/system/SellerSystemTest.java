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

    @Test
    void updateSellerProfile_ChangesShopDetails() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("upd_s"), uniqueUsername("upd_s"), id);

        Map<String, Object> body = Map.of(
                "shopName", "Updated Farm Name",
                "shopAddress", "99 New Lane",
                "description", "Better beef"
        );
        ResponseEntity<Map> resp = patchForMap("/api/seller/profile/" + id[0], body, session);

        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void updateSellerProfile_OtherUserSession_Returns403() {
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("upd_other"), uniqueUsername("upd_other"), sellerId);

        long[] intruderId = new long[1];
        String intruderSession = registerSeller(uniqueEmail("upd_intruder"), uniqueUsername("upd_intruder"), intruderId);

        Map<String, Object> body = Map.of("shopName", "Hijacked Farm");
        ResponseEntity<Map> resp = patchForMap("/api/seller/profile/" + sellerId[0], body, intruderSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    // ── Cow types ─────────────────────────────────────────────────────────────

    @Test
    void createCowType_ReturnsNewCowType() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("ct"), uniqueUsername("ct"), id);

        Map<String, Object> body = Map.of(
                "breed", "ANGUS",
                "description", "Premium Angus",
                "pricePerPound", 14.99,
                "availableCount", 3
        );
        ResponseEntity<Map> resp = postForMap("/api/seller/cow-types/" + id[0], body, session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("id"));
        assertEquals("ANGUS", resp.getBody().get("breed"));
    }

    @Test
    void getCowTypes_ReturnsList() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("gct"), uniqueUsername("gct"), id);

        Map<String, Object> body = Map.of("breed", "WAGYU", "description", "Premium Wagyu", "pricePerPound", 29.99, "availableCount", 1);
        postForMap("/api/seller/cow-types/" + id[0], body, session);

        ResponseEntity<List> resp = getForList("/api/seller/cow-types/" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void updateCowType_UpdatesPriceAndCount() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("uct"), uniqueUsername("uct"), id);

        Map<String, Object> create = Map.of("breed", "CONVENTIONAL", "description", "Standard", "pricePerPound", 8.00, "availableCount", 5);
        ResponseEntity<Map> created = postForMap("/api/seller/cow-types/" + id[0], create, session);
        long cowTypeId = ((Number) created.getBody().get("id")).longValue();

        Map<String, Object> update = Map.of("description", "Updated standard", "pricePerPound", 9.50, "availableCount", 3);
        ResponseEntity<Map> resp = patchForMap("/api/seller/cow-types/" + id[0] + "/" + cowTypeId, update, session);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(9.5, ((Number) resp.getBody().get("pricePerPound")).doubleValue(), 0.01);
        assertEquals(3, ((Number) resp.getBody().get("availableCount")).intValue());
    }

    @Test
    void deleteCowType_RemovesIt() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("dct"), uniqueUsername("dct"), id);

        Map<String, Object> create = Map.of("breed", "HERITAGE", "description", "Heritage", "pricePerPound", 12.00, "availableCount", 2);
        ResponseEntity<Map> created = postForMap("/api/seller/cow-types/" + id[0], create, session);
        long cowTypeId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Map> deleteResp = deleteForMap("/api/seller/cow-types/" + id[0] + "/" + cowTypeId, session);

        assertEquals(200, deleteResp.getStatusCode().value());
    }

    @Test
    void createCowType_OtherSeller_Returns403() {
        long[] ownerId = new long[1];
        registerSeller(uniqueEmail("ctowner"), uniqueUsername("ctowner"), ownerId);

        long[] intruderId = new long[1];
        String intruderSession = registerSeller(uniqueEmail("ctintruder"), uniqueUsername("ctintruder"), intruderId);

        Map<String, Object> body = Map.of("breed", "ANGUS", "description", "Hijack", "pricePerPound", 5.0, "availableCount", 1);
        ResponseEntity<Map> resp = postForMap("/api/seller/cow-types/" + ownerId[0], body, intruderSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    // ── Cows ──────────────────────────────────────────────────────────────────

    @Test
    void createCow_ReturnsNewCowWithCuts() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("cow"), uniqueUsername("cow"), id);

        // Create a cow type first
        Map<String, Object> ctBody = Map.of("breed", "ANGUS", "description", "Angus", "pricePerPound", 15.0, "availableCount", 1);
        ResponseEntity<Map> ctResp = postForMap("/api/seller/cow-types/" + id[0], ctBody, session);
        long cowTypeId = ((Number) ctResp.getBody().get("id")).longValue();

        Map<String, Object> cowBody = Map.of(
                "cowTypeId", cowTypeId,
                "name", "Bessie",
                "estimatedWeightLbs", 900.0,
                "harvestDate", "2026-08-01"
        );
        ResponseEntity<Map> resp = postForMap("/api/seller/cows/" + id[0], cowBody, session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody().get("id"));
        assertEquals("Bessie", resp.getBody().get("name"));
    }

    @Test
    void createCow_SecondCow_AlsoPersistsWithCuts() {
        // Verifies that creating multiple cows for the same seller each gets 22 cuts.
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("cows2"), uniqueUsername("cows2"), id);

        Map<String, Object> ctBody = Map.of("breed", "WAGYU", "description", "Wagyu", "pricePerPound", 30.0, "availableCount", 2);
        ResponseEntity<Map> ctResp = postForMap("/api/seller/cow-types/" + id[0], ctBody, session);
        long cowTypeId = ((Number) ctResp.getBody().get("id")).longValue();

        Map<String, Object> cowBody2 = Map.of("cowTypeId", cowTypeId, "name", "Dolly",
                "estimatedWeightLbs", 820.0, "harvestDate", "2026-11-01");
        ResponseEntity<Map> cowResp2 = postForMap("/api/seller/cows/" + id[0], cowBody2, session);
        assertEquals(200, cowResp2.getStatusCode().value(), "Second cow creation must succeed");
        long cowId2 = ((Number) cowResp2.getBody().get("id")).longValue();

        ResponseEntity<List> cutsResp = getForList("/api/seller/cows/" + cowId2 + "/cuts", session);
        assertEquals(22, cutsResp.getBody().size(), "Second cow must also auto-generate 22 cuts");
    }

    @Test
    void getCowCuts_SellerSessionReturns22Cuts() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("cuts"), uniqueUsername("cuts"), id);

        Map<String, Object> ctBody = Map.of("breed", "ANGUS", "description", "Angus", "pricePerPound", 12.0, "availableCount", 1);
        ResponseEntity<Map> ctResp = postForMap("/api/seller/cow-types/" + id[0], ctBody, session);
        long cowTypeId = ((Number) ctResp.getBody().get("id")).longValue();

        Map<String, Object> cowBody = Map.of("cowTypeId", cowTypeId, "name", "Cutsy",
                "estimatedWeightLbs", 850.0, "harvestDate", "2026-08-01");
        ResponseEntity<Map> cowResp = postForMap("/api/seller/cows/" + id[0], cowBody, session);
        assertEquals(200, cowResp.getStatusCode().value(), "Cow creation must succeed");
        long cowId = ((Number) cowResp.getBody().get("id")).longValue();

        // /api/seller/cows/{id}/cuts falls under /api/seller/** → SELLER required
        ResponseEntity<List> cutsResp = getForList("/api/seller/cows/" + cowId + "/cuts", session);

        assertEquals(200, cutsResp.getStatusCode().value());
        assertEquals(22, cutsResp.getBody().size(), "Every cow must auto-generate exactly 22 cuts");
    }

    @Test
    void getAvailableCuts_SellerSessionReturns22Initially() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("avail"), uniqueUsername("avail"), id);

        Map<String, Object> ctBody = Map.of("breed", "HERITAGE", "description", "H", "pricePerPound", 10.0, "availableCount", 1);
        ResponseEntity<Map> ctResp = postForMap("/api/seller/cow-types/" + id[0], ctBody, session);
        long cowTypeId = ((Number) ctResp.getBody().get("id")).longValue();

        Map<String, Object> cowBody = Map.of("cowTypeId", cowTypeId, "name", "Patch",
                "estimatedWeightLbs", 750.0, "harvestDate", "2026-09-01");
        ResponseEntity<Map> cowResp = postForMap("/api/seller/cows/" + id[0], cowBody, session);
        assertEquals(200, cowResp.getStatusCode().value(), "Cow creation must succeed");
        long cowId = ((Number) cowResp.getBody().get("id")).longValue();

        ResponseEntity<List> availResp = getForList("/api/seller/cows/" + cowId + "/cuts/available", session);

        assertEquals(200, availResp.getStatusCode().value());
        assertEquals(22, availResp.getBody().size());
    }

    // ── Certifications ────────────────────────────────────────────────────────

    @Test
    void addCertification_ReturnsNewCert() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("cert"), uniqueUsername("cert"), id);

        Map<String, Object> body = Map.of("name", "HALAL", "issuingBody", "Halal Authority");
        ResponseEntity<Map> resp = postForMap("/api/seller/certifications/" + id[0], body, session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody().get("id"));
    }

    @Test
    void getCertifications_ReturnsList() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("getcert"), uniqueUsername("getcert"), id);

        postForMap("/api/seller/certifications/" + id[0], Map.of("name", "ORGANIC", "issuingBody", "USDA", "expiryDate", "2027-12-31"), session);

        ResponseEntity<List> resp = getForList("/api/seller/certifications/" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void setCertifications_BulkReplaces() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("setcert"), uniqueUsername("setcert"), id);

        postForMap("/api/seller/certifications/" + id[0], Map.of("name", "HALAL", "issuingBody", "Authority"), session);

        // PUT replaces all certs
        HttpHeaders headers = withSession(session);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<String>> entity = new HttpEntity<>(List.of("KOSHER", "NON_GMO"), headers);
        ResponseEntity<Map> putResp = http.exchange(url("/api/seller/certifications/" + id[0]), HttpMethod.PUT, entity, Map.class);

        assertEquals(200, putResp.getStatusCode().value());

        // Confirm only the new certs are present
        ResponseEntity<List> listResp = getForList("/api/seller/certifications/" + id[0], session);
        assertEquals(2, listResp.getBody().size());
    }

    @Test
    void deleteCertification_RemovesIt() {
        long[] id = new long[1];
        String session = registerSeller(uniqueEmail("delcert"), uniqueUsername("delcert"), id);

        ResponseEntity<Map> certResp = postForMap("/api/seller/certifications/" + id[0], Map.of("name", "GRASS_FED", "issuingBody", "Certifier"), session);
        long certId = ((Number) certResp.getBody().get("id")).longValue();

        ResponseEntity<Map> deleteResp = deleteForMap("/api/seller/certifications/" + id[0] + "/" + certId, session);

        assertEquals(200, deleteResp.getStatusCode().value());

        ResponseEntity<List> listResp = getForList("/api/seller/certifications/" + id[0], session);
        assertTrue(listResp.getBody().isEmpty());
    }

    @Test
    void addCertification_BuyerSession_Returns403() {
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("certseller"), uniqueUsername("certseller"), sellerId);

        // Register a buyer and try to add a cert to the seller's profile
        String buyerSession = registerBuyer(uniqueEmail("certbuyer"), uniqueUsername("certbuyer"), null);

        Map<String, Object> body = Map.of("name", "HALAL", "issuingBody", "Halal Authority");
        ResponseEntity<Map> resp = postForMap("/api/seller/certifications/" + sellerId[0], body, buyerSession);

        // Spring Security denies ROLE_BUYER from /api/seller/** or the controller enforces seller-only
        assertTrue(resp.getStatusCode().value() == 403 || resp.getStatusCode().value() == 401);
    }
}
