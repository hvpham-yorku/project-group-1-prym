package com.prym.backend.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for the group–seller association API.
 *
 * Covers: request association, cancel, approve/deny, disassociation request,
 * confirm/deny disassociation, and access-control enforcement.
 *
 * NOTE: AssociationController /associate/{sellerId} expects the Seller entity PK
 *       (sellers.id), NOT the User ID. Use getSellerEntityId() before calling it.
 */
@SuppressWarnings("rawtypes")
class AssociationSystemTest extends SystemTestBase {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String setupBuyer(String prefix, long[] outId) {
        String session = registerBuyer(uniqueEmail(prefix), uniqueUsername(prefix), outId);
        postForMap("/api/buyer/profile", Map.of("userId", outId[0], "preferredCuts", "Ribeye"), session);
        return session;
    }

    /**
     * Returns the Seller entity's primary key (sellers.id), which differs from the user ID.
     * AssociationService.requestAssociation uses sellerRepository.findById(sellerId) —
     * i.e., it expects the Seller entity ID, not the User ID.
     * SellerController.getSellerProfile returns {"id": seller_entity_id, ...}.
     */
    private long getSellerEntityId(long userIdOfSeller, String sellerSession) {
        ResponseEntity<Map> resp = getForMap("/api/seller/profile/" + userIdOfSeller, sellerSession);
        assertEquals(200, resp.getStatusCode().value(), "Seller profile lookup must succeed");
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private long createGroup(long userId, String name, String session) {
        ResponseEntity<Map> resp = postForMap("/api/buyer/groups/create",
                Map.of("userId", userId, "name", name), session);
        assertEquals(200, resp.getStatusCode().value(), "Group creation must succeed");
        return ((Number) resp.getBody().get("groupId")).longValue();
    }

    // ── Request association ───────────────────────────────────────────────────

    @Test
    void requestAssociation_CreatorCanRequest() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("ra_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Association Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("ra_seller"), uniqueUsername("ra_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        ResponseEntity<Map> resp = postForMap(
                "/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody().get("associationId"));
    }

    @Test
    void requestAssociation_NonCreator_Returns400() {
        // AssociationService throws "Only the group creator can request" → controller wraps as 400.
        long[] ownerId = new long[1];
        String ownerSession = setupBuyer("ra_owner", ownerId);
        long groupId = createGroup(ownerId[0], "Owner Group", ownerSession);

        long[] memberId = new long[1];
        String memberSession = setupBuyer("ra_member", memberId);
        postForMap("/api/buyer/groups/join/" + groupId, Map.of("userId", memberId[0]), memberSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("ra_nseller"), uniqueUsername("ra_nseller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        ResponseEntity<Map> resp = postForMap(
                "/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", memberId[0]), memberSession);

        assertEquals(400, resp.getStatusCode().value());
    }

    // ── Cancel association request ────────────────────────────────────────────

    @Test
    void cancelAssociation_CreatorCanCancel() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("ca_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Cancel Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("ca_seller"), uniqueUsername("ca_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        ResponseEntity<Map> cancelResp = postForMap(
                "/api/buyer/groups/" + groupId + "/associate/cancel",
                Map.of("userId", buyerId[0]), buyerSession);

        assertEquals(200, cancelResp.getStatusCode().value());
    }

    // ── Get group association ─────────────────────────────────────────────────

    @Test
    void getGroupAssociation_PendingRequest_ReturnsAssociation() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("ga_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Get Assoc Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("ga_seller"), uniqueUsername("ga_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        ResponseEntity<Map> resp = getForMap(
                "/api/buyer/groups/" + groupId + "/association?userId=" + buyerId[0], buyerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isEmpty(), "Association object must be returned after a request");
    }

    @Test
    void getGroupAssociation_NoRequest_ReturnsEmptyMap() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("ga_none_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "No Assoc Group", buyerSession);

        ResponseEntity<Map> resp = getForMap(
                "/api/buyer/groups/" + groupId + "/association?userId=" + buyerId[0], buyerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody() == null || resp.getBody().isEmpty());
    }

    // ── Seller: pending requests ──────────────────────────────────────────────

    @Test
    void getSellerPendingRequests_ReturnsPendingList() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("spr_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Pending Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("spr_seller"), uniqueUsername("spr_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        ResponseEntity<List> resp = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertFalse(resp.getBody().isEmpty(), "Pending list must include the request just made");
    }

    @Test
    void getSellerPendingRequests_OtherSeller_Returns403() {
        long[] sellerId = new long[1];
        registerSeller(uniqueEmail("spr_owner"), uniqueUsername("spr_owner"), sellerId);

        long[] intruderId = new long[1];
        String intruderSession = registerSeller(uniqueEmail("spr_intruder"), uniqueUsername("spr_intruder"), intruderId);

        // 403 response body is a Map, not a List — use getForStatus to avoid deserialization error
        int status = getStatusForGet(
                "/api/seller/associations/pending?userId=" + sellerId[0], intruderSession);

        assertEquals(403, status);
    }

    // ── Seller: respond to association ────────────────────────────────────────

    @Test
    void sellerApprovesAssociation_StatusBecomesActive() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("approve_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Approve Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("approve_seller"), uniqueUsername("approve_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        ResponseEntity<List> pending = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);
        long assocId = ((Number) ((Map) pending.getBody().get(0)).get("associationId")).longValue();

        Map<String, Object> respondBody = Map.of("userId", sellerUserId[0], "action", "APPROVE");
        ResponseEntity<Map> resp = postForMap(
                "/api/seller/associations/" + assocId + "/respond", respondBody, sellerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("ASSOCIATED", resp.getBody().get("status"));
    }

    @Test
    void sellerDeniesAssociation_StatusBecomesDenied() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("deny_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Deny Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("deny_seller"), uniqueUsername("deny_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        ResponseEntity<List> pending = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);
        long assocId = ((Number) ((Map) pending.getBody().get(0)).get("associationId")).longValue();

        Map<String, Object> respondBody = Map.of("userId", sellerUserId[0], "action", "DENY");
        ResponseEntity<Map> resp = postForMap(
                "/api/seller/associations/" + assocId + "/respond", respondBody, sellerSession);

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("DENIED", resp.getBody().get("status"));
    }

    @Test
    void respondToAssociation_OtherSeller_Returns403() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("resp403_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Resp403 Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("resp403_seller"), uniqueUsername("resp403_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);

        ResponseEntity<List> pending = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);
        long assocId = ((Number) ((Map) pending.getBody().get(0)).get("associationId")).longValue();

        // A different seller tries to respond
        long[] intruderId = new long[1];
        String intruderSession = registerSeller(uniqueEmail("resp403_intruder"), uniqueUsername("resp403_intruder"), intruderId);

        Map<String, Object> respondBody = Map.of("userId", intruderId[0], "action", "APPROVE");
        ResponseEntity<Map> resp = postForMap(
                "/api/seller/associations/" + assocId + "/respond", respondBody, intruderSession);

        assertEquals(403, resp.getStatusCode().value());
    }

    // ── Disassociation ────────────────────────────────────────────────────────

    @Test
    void requestDisassociation_ThenSellerConfirms() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("dis_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Disassoc Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("dis_seller"), uniqueUsername("dis_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        // Create + approve association
        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);
        ResponseEntity<List> pending = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);
        long assocId = ((Number) ((Map) pending.getBody().get(0)).get("associationId")).longValue();
        postForMap("/api/seller/associations/" + assocId + "/respond",
                Map.of("userId", sellerUserId[0], "action", "APPROVE"), sellerSession);

        // Buyer requests disassociation
        ResponseEntity<Map> disResp = postForMap(
                "/api/buyer/groups/" + groupId + "/disassociate",
                Map.of("userId", buyerId[0]), buyerSession);
        assertEquals(200, disResp.getStatusCode().value());

        // Seller confirms
        ResponseEntity<Map> confirmResp = postForMap(
                "/api/seller/associations/" + assocId + "/respond-disassociation",
                Map.of("userId", sellerUserId[0], "action", "CONFIRM"), sellerSession);
        assertEquals(200, confirmResp.getStatusCode().value());
        assertEquals("DISASSOCIATED", confirmResp.getBody().get("status"));
    }

    @Test
    void requestDisassociation_ThenSellerDenies_StatusRemainsActive() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("dis_deny_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "Dis Deny Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("dis_deny_seller"), uniqueUsername("dis_deny_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        // Create + approve association
        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);
        ResponseEntity<List> pending = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);
        long assocId = ((Number) ((Map) pending.getBody().get(0)).get("associationId")).longValue();
        postForMap("/api/seller/associations/" + assocId + "/respond",
                Map.of("userId", sellerUserId[0], "action", "APPROVE"), sellerSession);

        // Buyer requests disassociation
        postForMap("/api/buyer/groups/" + groupId + "/disassociate",
                Map.of("userId", buyerId[0]), buyerSession);

        // Seller denies — status must go back to ACTIVE
        ResponseEntity<Map> denyResp = postForMap(
                "/api/seller/associations/" + assocId + "/respond-disassociation",
                Map.of("userId", sellerUserId[0], "action", "DENY"), sellerSession);
        assertEquals(200, denyResp.getStatusCode().value());
        assertEquals("ASSOCIATED", denyResp.getBody().get("status"));
    }

    // ── Seller: active associations ───────────────────────────────────────────

    @Test
    void getSellerAssociations_AfterApproval_ReturnsList() {
        long[] buyerId = new long[1];
        String buyerSession = setupBuyer("sa_buyer", buyerId);
        long groupId = createGroup(buyerId[0], "SA Group", buyerSession);

        long[] sellerUserId = new long[1];
        String sellerSession = registerSeller(uniqueEmail("sa_seller"), uniqueUsername("sa_seller"), sellerUserId);
        long sellerEntityId = getSellerEntityId(sellerUserId[0], sellerSession);

        postForMap("/api/buyer/groups/" + groupId + "/associate/" + sellerEntityId,
                Map.of("userId", buyerId[0]), buyerSession);
        ResponseEntity<List> pending = getForList(
                "/api/seller/associations/pending?userId=" + sellerUserId[0], sellerSession);
        long assocId = ((Number) ((Map) pending.getBody().get(0)).get("associationId")).longValue();
        postForMap("/api/seller/associations/" + assocId + "/respond",
                Map.of("userId", sellerUserId[0], "action", "APPROVE"), sellerSession);

        ResponseEntity<List> activeResp = getForList(
                "/api/seller/associations?userId=" + sellerUserId[0], sellerSession);

        assertEquals(200, activeResp.getStatusCode().value());
        assertFalse(activeResp.getBody().isEmpty(), "Active associations list must be non-empty after approval");
    }
}
