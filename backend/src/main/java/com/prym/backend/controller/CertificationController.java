package com.prym.backend.controller;

import com.prym.backend.model.Certification;
import com.prym.backend.model.User;
import com.prym.backend.service.CertificationService;
import com.prym.backend.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//Endpoints for managing a seller's certifications (Halal, Organic, etc).
//Supports adding one at a time, bulk-replacing all of them, fetching, and deleting.
@RestController
@RequestMapping("/api/seller/certifications")
public class CertificationController {

    private final CertificationService certificationService;
    private final SessionService sessionService;

    public CertificationController(CertificationService certificationService, SessionService sessionService) {
        this.certificationService = certificationService;
        this.sessionService = sessionService;
    }

    //adds a single certification with issuing body and expiry date
    @PostMapping("/{userId}")
    public ResponseEntity<?> addCertification(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

    	try {
        	validateIsSeller(sessionId, userId);
        } catch (RuntimeException e) {
        	return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            String name = request.get("name");
            String issuingBody = request.get("issuingBody");
            LocalDate expiryDate = request.get("expiryDate") != null ? LocalDate.parse(request.get("expiryDate")) : null;

            Certification cert = certificationService.addCertification(userId, name, issuingBody, expiryDate);
            return ResponseEntity.ok(cert);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //fetch all certs for a seller, used by the dashboard to show badges
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCertifications(
            @PathVariable Long userId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

    	try {
        	validateIsSeller(sessionId, userId);
        } catch (RuntimeException e) {
        	return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            List<Certification> certs = certificationService.getCertificationsBySeller(userId);
            return ResponseEntity.ok(certs);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    //bulk replace — wipes all existing certs and saves the new list.
    //the frontend uses this when the seller edits their profile.
    @PutMapping("/{userId}")
    public ResponseEntity<?> setCertifications(
            @PathVariable Long userId,
            @RequestBody List<String> certNames,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

    	try {
        	validateIsSeller(sessionId, userId);
        } catch (RuntimeException e) {
        	return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            certificationService.setCertifications(userId, certNames);
            return ResponseEntity.ok(Map.of("message", "Certifications updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //deletes one specific certification by its id
    @DeleteMapping("/{userId}/{certId}")
    public ResponseEntity<?> deleteCertification(
            @PathVariable Long userId,
            @PathVariable Long certId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        try {
        	validateIsSeller(sessionId, userId);
        } catch (RuntimeException e) {
        	return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }

        try {
            certificationService.deleteCertification(userId, certId);
            return ResponseEntity.ok(Map.of("message", "Certification deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private void validateIsSeller(String sessionId, Long userId) throws RuntimeException{
    	Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
        	throw new RuntimeException("Access denied");
        }

        if (sessionUser.get().getRole() != User.Role.SELLER) {
        	throw new RuntimeException("Only sellers can update certifications");
        }
        
    }
}
