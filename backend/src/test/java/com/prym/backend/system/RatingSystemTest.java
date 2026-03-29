package com.prym.backend.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for the ratings API.
 *
 * Covers: generate rating code, submit rating, view farm ratings (public),
 * invalid code rejection, and average rating calculation.
 */
@SuppressWarnings("rawtypes")
class RatingSystemTest extends SystemTestBase {

    // ── Generate code ─────────────────────────────────────────────────────────

    @Test
    void generateRatingCode_ReturnsValidCode() {
        long[] sellerId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("rc_gen"), uniqueUsername("rc_gen"), sellerId);

        Map<String, Object> body = Map.of("userId", sellerId[0]);
        ResponseEntity<Map> resp = postForMap("/api/ratings/generate-code", body, sellerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        String code = (String) resp.getBody().get("code");
        assertNotNull(code);
        assertTrue(code.startsWith("PRYM-"), "Rating code must start with 'PRYM-'");
        assertEquals(11, code.length(), "Rating code must be exactly 11 characters (PRYM- + 6)");
    }

    @Test
    void generateRatingCode_TwoCodes_AreUnique() {
        long[] sellerId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("rc_uniq"), uniqueUsername("rc_uniq"), sellerId);

        Map<String, Object> body = Map.of("userId", sellerId[0]);
        String code1 = (String) postForMap("/api/ratings/generate-code", body, sellerSession).getBody().get("code");
        String code2 = (String) postForMap("/api/ratings/generate-code", body, sellerSession).getBody().get("code");

        assertNotEquals(code1, code2, "Two generated rating codes must be unique");
    }

    @Test
    void generateRatingCode_OtherSeller_Returns403() {
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("rc_owner"), uniqueUsername("rc_owner"), sellerId);

        long[] intruderId = new long[1];
        String intruderSession = registerSeller(uniqueEmail("rc_intruder"), uniqueUsername("rc_intruder"), intruderId);

        Map<String, Object> body = Map.of("userId", sellerId[0]);
        ResponseEntity<Map> resp = postForMap("/api/ratings/generate-code", body, intruderSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void generateRatingCode_Buyer_Returns403() {
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("rc_bseller"), uniqueUsername("rc_bseller"), sellerId);

        String buyerSession = registerBuyer(uniqueEmail("rc_buyer"), uniqueUsername("rc_buyer"), null);

        Map<String, Object> body = Map.of("userId", sellerId[0]);
        ResponseEntity<Map> resp = postForMap("/api/ratings/generate-code", body, buyerSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    // ── Submit rating ─────────────────────────────────────────────────────────

    @Test
    void submitRating_ValidCode_Returns200() {
        long[] sellerId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("rs_seller"), uniqueUsername("rs_seller"), sellerId);

        String code = (String) postForMap("/api/ratings/generate-code", Map.of("userId", sellerId[0]), sellerSession)
                .getBody().get("code");

        long[] buyerId = new long[1];
        String buyerSession = registerBuyer(uniqueEmail("rs_buyer"), uniqueUsername("rs_buyer"), buyerId);
        // RatingService requires a Buyer profile to exist
        postForMap("/api/buyer/profile", Map.of("userId", buyerId[0], "preferredCuts", "Ribeye"), buyerSession);

        Map<String, Object> submitBody = Map.of("userId", buyerId[0], "code", code, "score", 5);
        ResponseEntity<Map> resp = postForMap("/api/ratings/submit", submitBody, buyerSession);

        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void submitRating_InvalidCode_Returns400() {
        long[] buyerId = new long[1];
        String buyerSession = registerBuyer(uniqueEmail("rs_inv"), uniqueUsername("rs_inv"), buyerId);
        // RatingService requires a Buyer profile to exist
        postForMap("/api/buyer/profile", Map.of("userId", buyerId[0], "preferredCuts", "Ribeye"), buyerSession);

        Map<String, Object> submitBody = Map.of("userId", buyerId[0], "code", "PRYM-XXXXXX", "score", 4);
        ResponseEntity<Map> resp = postForMap("/api/ratings/submit", submitBody, buyerSession);

        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void submitRating_Seller_Returns403() {
        long[] sellerId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("rs_sseller"), uniqueUsername("rs_sseller"), sellerId);

        String code = (String) postForMap("/api/ratings/generate-code", Map.of("userId", sellerId[0]), sellerSession)
                .getBody().get("code");

        // A seller tries to submit a rating — only buyers can submit
        Map<String, Object> submitBody = Map.of("userId", sellerId[0], "code", code, "score", 5);
        ResponseEntity<Map> resp = postForMap("/api/ratings/submit", submitBody, sellerSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    // ── Farm ratings (public) ─────────────────────────────────────────────────

    @Test
    void getFarmRatings_IsPublicAndReturnsAverageAndCount() {
        String username = uniqueUsername("fr_seller");
        long[] sellerId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("fr_seller"), username, sellerId);

        // Generate two codes and submit ratings
        String code1 = (String) postForMap("/api/ratings/generate-code", Map.of("userId", sellerId[0]), sellerSession).getBody().get("code");
        String code2 = (String) postForMap("/api/ratings/generate-code", Map.of("userId", sellerId[0]), sellerSession).getBody().get("code");

        long[] buyer1Id = new long[1];
        String buyer1Session = registerBuyer(uniqueEmail("fr_buyer1"), uniqueUsername("fr_buyer1"), buyer1Id);
        postForMap("/api/buyer/profile", Map.of("userId", buyer1Id[0], "preferredCuts", "Ribeye"), buyer1Session);
        postForMap("/api/ratings/submit", Map.of("userId", buyer1Id[0], "code", code1, "score", 5), buyer1Session);

        long[] buyer2Id = new long[1];
        String buyer2Session = registerBuyer(uniqueEmail("fr_buyer2"), uniqueUsername("fr_buyer2"), buyer2Id);
        postForMap("/api/buyer/profile", Map.of("userId", buyer2Id[0], "preferredCuts", "Brisket"), buyer2Session);
        postForMap("/api/ratings/submit", Map.of("userId", buyer2Id[0], "code", code2, "score", 3), buyer2Session);

        // Public endpoint — no session needed
        ResponseEntity<Map> resp = getForMap("/api/ratings/" + username, null);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(4.0, ((Number) resp.getBody().get("averageRating")).doubleValue(), 0.001,
                "Average of 5 and 3 must be 4.0");
        assertEquals(2, ((Number) resp.getBody().get("totalRatings")).intValue());
    }

    @Test
    void getFarmRatings_NoRatingsYet_ReturnsZeroValues() {
        String username = uniqueUsername("fr_norating");
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("fr_norating"), username, sellerId);

        ResponseEntity<Map> resp = getForMap("/api/ratings/" + username, null);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        double avg = ((Number) resp.getBody().get("averageRating")).doubleValue();
        int total = ((Number) resp.getBody().get("totalRatings")).intValue();
        assertEquals(0.0, avg, 0.001);
        assertEquals(0, total);
    }
}
