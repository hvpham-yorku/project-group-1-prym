package com.prym.backend.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for the group API (buyer-only).
 *
 * Covers: create group, list available/own groups, join group, get by ID,
 * get by invite code, regenerate invite code, update cuts, leave group,
 * matching farms, and access-control enforcement.
 *
 * NOTE: The group DTO uses "groupId" and "groupName" (not "id"/"name").
 *       The "inviteCode" field is only present for members (alreadyJoined=true).
 */
@SuppressWarnings("rawtypes")
class GroupSystemTest extends SystemTestBase {

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Registers a buyer, creates their buyer profile, returns session. outId[0] = userId. */
    private String setupBuyer(String prefix, long[] outId) {
        String session = registerBuyer(uniqueEmail(prefix), uniqueUsername(prefix), outId);
        postForMap("/api/buyer/profile", Map.of("userId", outId[0], "preferredCuts", "Ribeye"), session);
        return session;
    }

    /** Creates a group and returns the group DTO map. Uses "groupId" key for the id. */
    private Map<String, Object> createGroup(long userId, String name, String session) {
        Map<String, Object> body = Map.of("userId", userId, "name", name);
        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/create", body, session);
        assertEquals(200, resp.getStatusCode().value(), "Group creation must succeed");
        @SuppressWarnings("unchecked")
        Map<String, Object> g = (Map<String, Object>) resp.getBody();
        return g;
    }

  
    // ── Matching farms ────────────────────────────────────────────────────────

    @Test
    void getMatchingFarms_Returns200() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("mf_buyer", buyerId);
        Map<String, Object> group = createGroup(buyerId[0], "Matching Farms Group", buyerSession);
        long groupId = ((Number) group.get("groupId")).longValue();

        // Register a seller so there is at least something to match against
        registerSeller(uniqueEmail("mf_seller"), uniqueUsername("mf_seller"), null);

        ResponseEntity<Map> resp = getForMap(
                "/api/buyer/groups/" + groupId + "/matching-farms?userId=" + buyerId[0], buyerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
    }


}