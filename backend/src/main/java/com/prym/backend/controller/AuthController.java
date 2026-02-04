package com.prym.backend.controller;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/buyer")
    public ResponseEntity<?> registerBuyer(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User user = authService.register(email, password, User.Role.BUYER);

            return ResponseEntity.ok(buildUserResponse(user));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/seller")
    public ResponseEntity<?> registerSeller(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User user = authService.register(email, password, User.Role.SELLER);

            return ResponseEntity.ok(buildUserResponse(user));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOptional = authService.login(email, password);

        if (userOptional.isPresent()) {
            return ResponseEntity.ok(buildUserResponse(userOptional.get()));
        }

        return ResponseEntity.status(401).body("Invalid email or password");
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return response;
    }
}