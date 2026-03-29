package com.prym.backend.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for the authentication API.
 *
 * Covers: register (buyer & seller), login, /me, logout, update user.
 * Each request goes through the full HTTP stack: Servlet → Security → Controller → Service → H2.
 */
class AuthSystemTest extends SystemTestBase {

    // ── Registration ──────────────────────────────────────────────────────────

    @Test
    void registerBuyer_ReturnsUserDataAndSessionCookie() {
        String email = uniqueEmail("buyer");
        long[] id = new long[1];
        String session = registerBuyer(email, uniqueUsername("buyer"), id);

        assertNotNull(session, "Registration must return a SESSION_ID cookie");
        assertTrue(id[0] > 0, "Response must include the new user's id");
    }

    @Test
    void registerSeller_ReturnsUserDataAndSessionCookie() {
        String email = uniqueEmail("seller");
        long[] id = new long[1];
        String session = registerSeller(email, uniqueUsername("seller"), id);

        assertNotNull(session, "Registration must return a SESSION_ID cookie");
        assertTrue(id[0] > 0, "Response must include the new user's id");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void registerBuyer_DuplicateEmail_Returns400() {
        String email = uniqueEmail("dup");
        registerBuyer(email, uniqueUsername("dup1"), null);

        Map<String, String> req = Map.of(
                "email", email,
                "password", "testPass123",
                "username", uniqueUsername("dup2"),
                "firstName", "Test",
                "lastName", "Buyer",
                "phoneNumber", "4165550000",
                "zipCode", "10001"
        );
        ResponseEntity<Map> resp = http.postForEntity(url("/api/auth/register/buyer"), req, Map.class);

        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void registerBuyer_MissingEmail_Returns400() {
        Map<String, String> req = Map.of(
                "password", "testPass123",
                "username", uniqueUsername("nomail"),
                "firstName", "Test",
                "lastName", "Buyer",
                "phoneNumber", "4165550000",
                "zipCode", "10001"
        );
        ResponseEntity<Map> resp = http.postForEntity(url("/api/auth/register/buyer"), req, Map.class);

        assertEquals(400, resp.getStatusCode().value());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_ValidCredentials_ReturnsSessionAndUserId() {
        String email = uniqueEmail("loginok");
        long[] regId = new long[1];
        registerBuyer(email, uniqueUsername("loginok"), regId);

        long[] loginId = new long[1];
        String session = login(email, "testPass123", loginId);

        assertNotNull(session, "Login must return a SESSION_ID cookie");
        assertEquals(regId[0], loginId[0], "Login must return the same user id as registration");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void login_WrongPassword_Returns401() {
        String email = uniqueEmail("badpw");
        registerBuyer(email, uniqueUsername("badpw"), null);

        ResponseEntity<Map> resp = http.postForEntity(
                url("/api/auth/login"),
                Map.of("email", email, "password", "wrongPassword"),
                Map.class);

        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void login_UnknownEmail_Returns401() {
        ResponseEntity<Map> resp = http.postForEntity(
                url("/api/auth/login"),
                Map.of("email", "nobody@nowhere.com", "password", "anything"),
                Map.class);

        assertEquals(401, resp.getStatusCode().value());
    }

    // ── /me ───────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("rawtypes")
    void me_WithValidSession_ReturnsCurrentUser() {
        String email = uniqueEmail("me");
        long[] id = new long[1];
        String session = registerBuyer(email, uniqueUsername("me"), id);

        ResponseEntity<Map> resp = getForMap("/api/auth/me", session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(id[0], ((Number) resp.getBody().get("id")).longValue());
        assertEquals(email, resp.getBody().get("email"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void me_WithoutSession_Returns401() {
        ResponseEntity<Map> resp = getForMap("/api/auth/me", null);

        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void me_WithBogusSession_Returns401() {
        ResponseEntity<Map> resp = getForMap("/api/auth/me", "totally-fake-session-id");

        assertEquals(401, resp.getStatusCode().value());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("rawtypes")
    void logout_ThenMeReturns401() {
        String email = uniqueEmail("logout");
        String session = registerBuyer(email, uniqueUsername("logout"), null);

        // Confirm we're logged in
        ResponseEntity<Map> before = getForMap("/api/auth/me", session);
        assertEquals(200, before.getStatusCode().value());

        // Logout
        ResponseEntity<Map> logoutResp = postForMap("/api/auth/logout", null, session);
        assertEquals(200, logoutResp.getStatusCode().value());

        // Session must now be invalid
        ResponseEntity<Map> after = getForMap("/api/auth/me", session);
        assertEquals(401, after.getStatusCode().value());
    }

    // ── Update user ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("rawtypes")
    void updateUser_ChangesFirstName() {
        String email = uniqueEmail("upd");
        String session = registerBuyer(email, uniqueUsername("upd"), null);

        Map<String, String> body = Map.of("firstName", "Updated");
        ResponseEntity<Map> resp = patchForMap("/api/auth/user", body, session);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("Updated", resp.getBody().get("firstName"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void updateUser_WithoutSession_Returns401() {
        Map<String, String> body = Map.of("firstName", "Ghost");
        ResponseEntity<Map> resp = patchForMap("/api/auth/user", body, null);

        assertEquals(401, resp.getStatusCode().value());
    }
}
