package com.prym.backend.system;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all system tests.
 *
 * Boots the full Spring application on a random port against H2 in-memory DB.
 * Uses a plain RestTemplate configured to never throw on 4xx/5xx — all tests
 * inspect status codes manually so error scenarios can be properly asserted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SystemTestBase {

    @LocalServerPort
    protected int port;

    // Plain RestTemplate that does NOT throw on 4xx / 5xx responses.
    protected final RestTemplate http = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        RestTemplate rt = new RestTemplate(new JdkClientHttpRequestFactory());
        rt.setErrorHandler(new NoOpResponseErrorHandler());
        return rt;
    }

    // ── URL helper ────────────────────────────────────────────────────────

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ── Cookie helpers ────────────────────────────────────────────────────

    /** Extract the SESSION_ID value from a Set-Cookie response header. */
    protected String extractSessionCookie(HttpHeaders headers) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies == null) return null;
        for (String cookie : cookies) {
            for (String part : cookie.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("SESSION_ID=")) {
                    return trimmed.substring("SESSION_ID=".length());
                }
            }
        }
        return null;
    }

    /** Build request headers that carry the SESSION_ID cookie. */
    protected HttpHeaders withSession(String sessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionId != null) {
            headers.set("Cookie", "SESSION_ID=" + sessionId);
        }
        return headers;
    }

    // ── Registration / login helpers ──────────────────────────────────────

    /** Registers a buyer; returns session cookie. outId[0] is set to the user ID. */
    protected String registerBuyer(String email, String username, long[] outId) {
        Map<String, String> req = new HashMap<>();
        req.put("email", email);
        req.put("password", "testPass123");
        req.put("username", username);
        req.put("firstName", "Test");
        req.put("lastName", "Buyer");
        req.put("phoneNumber", "4165550000");
        req.put("zipCode", "10001");

        ResponseEntity<Map> resp = http.postForEntity(url("/api/auth/register/buyer"), req, Map.class);
        if (outId != null && resp.getBody() != null) {
            outId[0] = ((Number) resp.getBody().get("id")).longValue();
        }
        return extractSessionCookie(resp.getHeaders());
    }

    /** Registers a seller; returns session cookie. outId[0] is set to the user ID. */
    protected String registerSeller(String email, String username, long[] outId) {
        Map<String, String> req = new HashMap<>();
        req.put("email", email);
        req.put("password", "testPass123");
        req.put("username", username);
        req.put("firstName", "Test");
        req.put("lastName", "Seller");
        req.put("phoneNumber", "4165550001");
        req.put("zipCode", "M5V3A8");
        req.put("shopName", username + " Farm");

        ResponseEntity<Map> resp = http.postForEntity(url("/api/auth/register/seller"), req, Map.class);
        if (outId != null && resp.getBody() != null) {
            outId[0] = ((Number) resp.getBody().get("id")).longValue();
        }
        return extractSessionCookie(resp.getHeaders());
    }

    /** Logs in an existing user; returns session cookie. outId[0] is set to the user ID. */
    protected String login(String email, String password, long[] outId) {
        ResponseEntity<Map> resp = http.postForEntity(
                url("/api/auth/login"), Map.of("email", email, "password", password), Map.class);
        if (outId != null && resp.getBody() != null && resp.getBody().containsKey("id")) {
            outId[0] = ((Number) resp.getBody().get("id")).longValue();
        }
        return extractSessionCookie(resp.getHeaders());
    }

    /** Unique email per test run to avoid collisions with seed data. */
    protected String uniqueEmail(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@sysTest.com";
    }

    /** Unique username per test run. */
    protected String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ── Typed request helpers ─────────────────────────────────────────────

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> getForMap(String path, String sessionId) {
        HttpEntity<Void> entity = new HttpEntity<>(withSession(sessionId));
        return http.exchange(url(path), HttpMethod.GET, entity, Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<List> getForList(String path, String sessionId) {
        HttpEntity<Void> entity = new HttpEntity<>(withSession(sessionId));
        return http.exchange(url(path), HttpMethod.GET, entity, List.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> postForMap(String path, Object body, String sessionId) {
        HttpEntity<Object> entity = new HttpEntity<>(body, withSession(sessionId));
        return http.exchange(url(path), HttpMethod.POST, entity, Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> patchForMap(String path, Object body, String sessionId) {
        HttpEntity<Object> entity = new HttpEntity<>(body, withSession(sessionId));
        return http.exchange(url(path), HttpMethod.PATCH, entity, Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> deleteForMap(String path, String sessionId) {
        HttpEntity<Void> entity = new HttpEntity<>(withSession(sessionId));
        return http.exchange(url(path), HttpMethod.DELETE, entity, Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> deleteForMapWithBody(String path, Object body, String sessionId) {
        HttpEntity<Object> entity = new HttpEntity<>(body, withSession(sessionId));
        return http.exchange(url(path), HttpMethod.DELETE, entity, Map.class);
    }

    /** Returns just the HTTP status code for a GET request — safe even when the response body is not a List. */
    protected int getStatusForGet(String path, String sessionId) {
        HttpEntity<Void> entity = new HttpEntity<>(withSession(sessionId));
        return http.exchange(url(path), HttpMethod.GET, entity, String.class).getStatusCode().value();
    }
}
