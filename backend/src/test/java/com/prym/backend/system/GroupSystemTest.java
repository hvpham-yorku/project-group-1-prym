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

    // ── Create group ──────────────────────────────────────────────────────────

    @Test
    void createGroup_ReturnsGroupWithInviteCode() {
        long[] id = new long[1];
        String session = setupBuyer("cg", id);

        Map<String, Object> group = createGroup(id[0], "Test Group", session);

        // Group DTO uses "groupId" and "groupName", not "id" / "name"
        assertNotNull(group.get("groupId"), "Created group must have a groupId");
        assertNotNull(group.get("inviteCode"), "Creator is a member so inviteCode must be present");
        assertEquals("Test Group", group.get("groupName"));
    }

    @Test
    void createGroup_OtherUser_Returns403() {
        long[] id1 = new long[1];
        setupBuyer("cg_own", id1);

        long[] id2 = new long[1];
        String session2 = setupBuyer("cg_int", id2);

        Map<String, Object> body = Map.of("userId", id1[0], "name", "Hijacked Group");
        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/create", body, session2);

        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void createGroup_Seller_Returns403() {
        long[] sellerId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("cg_seller"), uniqueUsername("cg_seller"), sellerId);

        Map<String, Object> body = Map.of("userId", sellerId[0], "name", "Seller Group");
        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/create", body, sellerSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    // ── List groups ───────────────────────────────────────────────────────────

    @Test
    void getAvailableGroups_ExcludesOwnGroup() {
        long[] id = new long[1];
        String session = setupBuyer("avail_g", id);
        createGroup(id[0], "My Own Group", session);

        ResponseEntity<List> resp = getForList("/api/buyer/groups?userId=" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        // The group just created by this user must not appear in "available" (they own it)
        for (Object item : resp.getBody()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> g = (Map<String, Object>) item;
            assertNotEquals("My Own Group", g.get("groupName"),
                    "Creator's own group must not appear in the available list");
        }
    }

    @Test
    void getMyGroups_ReturnsMemberGroups() {
        long[] id = new long[1];
        String session = setupBuyer("mine_g", id);
        createGroup(id[0], "Mine Group", session);

        ResponseEntity<List> resp = getForList("/api/buyer/groups/mine?userId=" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        boolean found = false;
        for (Object item : resp.getBody()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> g = (Map<String, Object>) item;
            if ("Mine Group".equals(g.get("groupName"))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "The group created by the user must appear in their 'mine' list");
    }

    // ── Get group by ID ───────────────────────────────────────────────────────

    @Test
    void getGroupById_MemberCanAccess() {
        long[] id = new long[1];
        String session = setupBuyer("gbid", id);
        Map<String, Object> group = createGroup(id[0], "Get By Id Group", session);
        long groupId = ((Number) group.get("groupId")).longValue();

        ResponseEntity<Map> resp = getForMap("/api/buyer/groups/" + groupId + "?userId=" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Get By Id Group", resp.getBody().get("groupName"));
    }

    @Test
    void getGroupById_NonMember_Returns200WithAlreadyJoinedFalse() {
        // GroupController does not enforce membership — any buyer can view any group DTO.
        // Non-members see the group but alreadyJoined=false.
        long[] ownerId = new long[1];
        String ownerSession = setupBuyer("gbid_owner", ownerId);
        Map<String, Object> group = createGroup(ownerId[0], "Private Group", ownerSession);
        long groupId = ((Number) group.get("groupId")).longValue();

        long[] otherId = new long[1];
        String otherSession = setupBuyer("gbid_other", otherId);

        ResponseEntity<Map> resp = getForMap("/api/buyer/groups/" + groupId + "?userId=" + otherId[0], otherSession);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(Boolean.FALSE, resp.getBody().get("alreadyJoined"),
                "Non-member must see alreadyJoined=false");
    }

    // ── Join group ────────────────────────────────────────────────────────────

    @Test
    void joinGroup_AllowsNonMemberToJoin() {
        long[] ownerId = new long[1];
        String ownerSession = setupBuyer("join_owner", ownerId);
        Map<String, Object> group = createGroup(ownerId[0], "Joinable Group", ownerSession);
        long groupId = ((Number) group.get("groupId")).longValue();

        long[] joinerId = new long[1];
        String joinerSession = setupBuyer("join_joiner", joinerId);

        Map<String, Object> joinBody = Map.of("userId", joinerId[0]);
        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/join/" + groupId, joinBody, joinerSession);

        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void joinGroup_ThenAppearsInMyGroups() {
        long[] ownerId = new long[1];
        String ownerSession = setupBuyer("join_mine_owner", ownerId);
        Map<String, Object> group = createGroup(ownerId[0], "Joined Group", ownerSession);
        long groupId = ((Number) group.get("groupId")).longValue();

        long[] joinerId = new long[1];
        String joinerSession = setupBuyer("join_mine_joiner", joinerId);

        postForMap("/api/buyer/groups/join/" + groupId, Map.of("userId", joinerId[0]), joinerSession);

        ResponseEntity<List> mine = getForList("/api/buyer/groups/mine?userId=" + joinerId[0], joinerSession);
        boolean found = false;
        for (Object item : mine.getBody()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> g = (Map<String, Object>) item;
            if (groupId == ((Number) g.get("groupId")).longValue()) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Joined group must appear in the joiner's 'mine' list");
    }

    // ── Get group by invite code ───────────────────────────────────────────────

    @Test
    void getGroupByCode_MemberCanLookUp() {
        long[] id = new long[1];
        String session = setupBuyer("bycode", id);
        Map<String, Object> group = createGroup(id[0], "Code Group", session);
        String inviteCode = (String) group.get("inviteCode");
        assertNotNull(inviteCode, "Creator must receive the inviteCode in the group DTO");

        ResponseEntity<Map> resp = getForMap("/api/buyer/groups/by-code/" + inviteCode + "?userId=" + id[0], session);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals(inviteCode, resp.getBody().get("inviteCode"));
    }

    // ── Regenerate invite code ────────────────────────────────────────────────

    @Test
    void regenerateInviteCode_CreatorGetsNewCode() {
        long[] id = new long[1];
        String session = setupBuyer("regen", id);
        Map<String, Object> group = createGroup(id[0], "Regen Group", session);
        long groupId = ((Number) group.get("groupId")).longValue();
        String oldCode = (String) group.get("inviteCode");

        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/" + groupId + "/regenerate-code",
                Map.of("userId", id[0]), session);

        assertEquals(200, resp.getStatusCode().value());
        String newCode = (String) resp.getBody().get("inviteCode");
        assertNotNull(newCode);
        assertNotEquals(oldCode, newCode, "Regenerated code must differ from the old one");
    }

    @Test
    void regenerateInviteCode_NonCreator_Returns400() {
        // GroupService throws RuntimeException("Only the group creator can regenerate...")
        // GroupController catches it and returns 400 (not 403).
        long[] ownerId = new long[1];
        String ownerSession = setupBuyer("regen_owner", ownerId);
        Map<String, Object> group = createGroup(ownerId[0], "Regen Owner Group", ownerSession);
        long groupId = ((Number) group.get("groupId")).longValue();

        long[] memberId = new long[1];
        String memberSession = setupBuyer("regen_member", memberId);
        postForMap("/api/buyer/groups/join/" + groupId, Map.of("userId", memberId[0]), memberSession);

        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/" + groupId + "/regenerate-code",
                Map.of("userId", memberId[0]), memberSession);

        assertEquals(400, resp.getStatusCode().value());
    }

    // ── Leave group ───────────────────────────────────────────────────────────

    @Test
    void leaveGroup_MemberCanLeave() {
        long[] ownerId = new long[1];
        String ownerSession = setupBuyer("leave_owner", ownerId);
        Map<String, Object> group = createGroup(ownerId[0], "Leave Group", ownerSession);
        long groupId = ((Number) group.get("groupId")).longValue();

        long[] memberId = new long[1];
        String memberSession = setupBuyer("leave_member", memberId);
        postForMap("/api/buyer/groups/join/" + groupId, Map.of("userId", memberId[0]), memberSession);

        ResponseEntity<Map> leaveResp = postForMap("/api/buyer/groups/leave/" + groupId,
                Map.of("userId", memberId[0]), memberSession);

        assertEquals(200, leaveResp.getStatusCode().value());
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

    // ── Update cuts ───────────────────────────────────────────────────────────

    @Test
    void updateGroupCuts_Returns200() {
        long[] id = new long[1];
        String session = setupBuyer("cuts_g", id);
        Map<String, Object> group = createGroup(id[0], "Cuts Group", session);
        long groupId = ((Number) group.get("groupId")).longValue();

        Map<String, Object> body = Map.of("userId", id[0], "cuts", "Ribeye x1, Brisket x1");
        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/cuts/" + groupId, body, session);

        assertEquals(200, resp.getStatusCode().value());
    }
}
