package com.prym.backend.controller;

import com.prym.backend.model.Session;
import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
@RestController //handles API requests
@RequestMapping("/api/auth") //sets the base path, helps us to avoid writing the full path when using @Postmapping
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;

    public AuthController(AuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register/buyer")
    public ResponseEntity<?> registerBuyer(@RequestBody Map<String, String> request, HttpServletResponse response) {// ResponseEntity<?> = The response we send back (can be any type, that's what ? means)
// @RequestBody Map<String, String> request = The JSON data sent by frontend, converted to a Map
// HttpServletResponse response = Lets us add cookies to the browser
        try {
            String email = request.get("email");// Get "email" value from the Map
            String password = request.get("password"); // Get "password" value from the Map

            User user = authService.register(email, password, User.Role.BUYER);//AuthService checks if email exists, hashes the password, saves new user to the database, returns the created User object
            Session session = sessionService.createSession(user);//generate a new session for this user which generates a random Id (UUID) , save session to database, and returns the session object to be stored in variable

            addSessionCookie(response, session.getSessionId());// Add a cookie to the browser response. The browser will send this cookie with every future request

            return ResponseEntity.ok(buildUserResponse(user)); // Return HTTP 200 (success) with user data as JSON

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // If anything fails (like email already exists), we return HTTP 400 (bad request) with error message
        }
    }

    @PostMapping("/register/seller")
    public ResponseEntity<?> registerSeller(@RequestBody Map<String, String> request, HttpServletResponse response) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User user = authService.register(email, password, User.Role.SELLER);
            Session session = sessionService.createSession(user);

            addSessionCookie(response, session.getSessionId());

            return ResponseEntity.ok(buildUserResponse(user));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOptional = authService.login(email, password);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Session session = sessionService.createSession(user);

            addSessionCookie(response, session.getSessionId());

            return ResponseEntity.ok(buildUserResponse(user));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "SESSION_ID", required = false) String sessionId, HttpServletResponse response) {
        if (sessionId != null) {
            sessionService.deleteSession(sessionId);
        }

        // Clear the cookie
        Cookie cookie = new Cookie("SESSION_ID", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete immediately
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(name = "SESSION_ID", required = false) String sessionId) {
        if (sessionId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Optional<User> userOptional = sessionService.validateSession(sessionId);

        if (userOptional.isPresent()) {
            return ResponseEntity.ok(buildUserResponse(userOptional.get()));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Session expired"));
    }

private void addSessionCookie(HttpServletResponse response, String sessionId) {
    ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId)
            .httpOnly(true)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .sameSite("Lax")
            .build();
    response.addHeader("Set-Cookie", cookie.toString());
}

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return response;
    }
}