package com.prym.backend.controller;

import com.prym.backend.model.CowType;
import com.prym.backend.model.User;
import com.prym.backend.service.CowTypeService;
import com.prym.backend.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

//CRUD endpoints for managing cow types (breeds) that a seller offers.
//All endpoints verify the session cookie matches the userId in the path
//so sellers can only mess with their own stuff.
@RestController
@RequestMapping("/api/seller/cow-types")
public class CowTypeController {

    private final CowTypeService cowTypeService;
    private final SessionService sessionService;

    public CowTypeController(CowTypeService cowTypeService, SessionService sessionService) {
        this.cowTypeService = cowTypeService;
        this.sessionService = sessionService;
    }

    //create a new breed/type listing for this seller
    @PostMapping("/{userId}")
    public ResponseEntity<?> createCowType(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            String breed = (String) request.get("breed");
            String description = (String) request.get("description");
            Double pricePerPound = request.get("pricePerPound") != null
                    ? Double.parseDouble(request.get("pricePerPound").toString()) : null;
            Integer availableCount = request.get("availableCount") != null
                    ? Integer.parseInt(request.get("availableCount").toString()) : null;

            CowType cowType = cowTypeService.createCowType(userId, breed, description, pricePerPound, availableCount);
            return ResponseEntity.ok(cowType);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //get all cow types belonging to this seller
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCowTypes(
            @PathVariable Long userId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            List<CowType> cowTypes = cowTypeService.getCowTypesBySeller(userId);
            return ResponseEntity.ok(cowTypes);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    //partial update for a cow type, only changes non-null fields
    @PatchMapping("/{userId}/{cowTypeId}")
    public ResponseEntity<?> updateCowType(
            @PathVariable Long userId,
            @PathVariable Long cowTypeId,
            @RequestBody Map<String, Object> request,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            String breed = (String) request.get("breed");
            String description = (String) request.get("description");
            Double pricePerPound = request.get("pricePerPound") != null
                    ? Double.parseDouble(request.get("pricePerPound").toString()) : null;
            Integer availableCount = request.get("availableCount") != null
                    ? Integer.parseInt(request.get("availableCount").toString()) : null;

            CowType updated = cowTypeService.updateCowType(cowTypeId, breed, description, pricePerPound, availableCount);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //permanently removes a cow type, hopefully the seller meant to do this
    @DeleteMapping("/{userId}/{cowTypeId}")
    public ResponseEntity<?> deleteCowType(
            @PathVariable Long userId,
            @PathVariable Long cowTypeId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            cowTypeService.deleteCowType(cowTypeId);
            return ResponseEntity.ok(Map.of("message", "CowType deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
