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

@RestController
@RequestMapping("/api/seller/certifications")
public class CertificationController {

    private final CertificationService certificationService;
    private final SessionService sessionService;

    public CertificationController(CertificationService certificationService, SessionService sessionService) {
        this.certificationService = certificationService;
        this.sessionService = sessionService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> addCertification(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
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

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCertifications(
            @PathVariable Long userId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            List<Certification> certs = certificationService.getCertificationsBySeller(userId);
            return ResponseEntity.ok(certs);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/{certId}")
    public ResponseEntity<?> deleteCertification(
            @PathVariable Long userId,
            @PathVariable Long certId,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) {

        Optional<User> sessionUser = sessionService.validateSession(sessionId);
        if (sessionUser.isEmpty() || !sessionUser.get().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            certificationService.deleteCertification(certId);
            return ResponseEntity.ok(Map.of("message", "Certification deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
