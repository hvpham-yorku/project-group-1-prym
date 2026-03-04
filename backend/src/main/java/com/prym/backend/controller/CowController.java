package com.prym.backend.controller;

import com.prym.backend.model.Cow;
import com.prym.backend.model.CowCut;
import com.prym.backend.model.User;
import com.prym.backend.service.CowService;
import com.prym.backend.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/seller/cows")
public class CowController {

    private final CowService cowService;
    private final SessionService sessionService;

    public CowController(CowService cowService, SessionService sessionService) {
        this.cowService = cowService;
        this.sessionService = sessionService;
    }

    // Seller creates a specific cow — triggers auto-generation of 22 CowCut records
    @PostMapping("/{userId}")
    public ResponseEntity<?> createCow(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            Long cowTypeId = Long.parseLong(request.get("cowTypeId").toString());
            String name = (String) request.get("name");
            Double estimatedWeightLbs = request.get("estimatedWeightLbs") != null
                    ? Double.parseDouble(request.get("estimatedWeightLbs").toString()) : null;
            LocalDate harvestDate = request.get("harvestDate") != null
                    ? LocalDate.parse((String) request.get("harvestDate")) : null;

            Cow cow = cowService.createCow(cowTypeId, name, estimatedWeightLbs, harvestDate);
            return ResponseEntity.ok(cow);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get all cows belonging to a seller
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCows(
            @PathVariable Long userId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            List<Cow> cows = cowService.getCowsBySeller(userId);
            return ResponseEntity.ok(cows);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // Get all cuts for a cow (both AVAILABLE and CLAIMED)
    @GetMapping("/{cowId}/cuts")
    public ResponseEntity<?> getAllCuts(@PathVariable Long cowId) {
        try {
            List<CowCut> cuts = cowService.getAllCuts(cowId);
            return ResponseEntity.ok(cuts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // Get only unclaimed (AVAILABLE) cuts for a cow — used by the matching system
    @GetMapping("/{cowId}/cuts/available")
    public ResponseEntity<?> getAvailableCuts(@PathVariable Long cowId) {
        try {
            List<CowCut> cuts = cowService.getAvailableCuts(cowId);
            return ResponseEntity.ok(cuts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
