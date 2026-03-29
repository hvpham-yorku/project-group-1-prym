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