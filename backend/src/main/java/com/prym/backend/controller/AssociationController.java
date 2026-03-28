package com.prym.backend.controller;

import com.prym.backend.model.AssociationStatus;
import com.prym.backend.model.User;
import com.prym.backend.repository.GroupMessageRepository;
import com.prym.backend.repository.GroupSellerAssociationRepository;
import com.prym.backend.repository.SellerRepository;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.AssociationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Handles group-seller association requests.
// Buyer routes live under /api/buyer (BUYER role), seller routes under /api/seller (SELLER role).
@RestController
public class AssociationController {

    private final AssociationService associationService;
    private final UserRepository userRepository;
    private final GroupSellerAssociationRepository associationRepository;
    private final SellerRepository sellerRepository;
    private final GroupMessageRepository groupMessageRepository;

    public AssociationController(AssociationService associationService,
                                 UserRepository userRepository,
                                 GroupSellerAssociationRepository associationRepository,
                                 SellerRepository sellerRepository,
                                 GroupMessageRepository groupMessageRepository) {
        this.associationService = associationService;
        this.userRepository = userRepository;
        this.associationRepository = associationRepository;
        this.sellerRepository = sellerRepository;
        this.groupMessageRepository = groupMessageRepository;
    }

    private ResponseEntity<?> errorResponse(RuntimeException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("not found")) return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        if (msg.contains("access denied")) return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private Long getLoggedInUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // ── Buyer-side ────────────────────────────────────────────────────────

    // POST /api/buyer/groups/{groupId}/associate/{sellerId}
    // Send an association request to a seller.
    @PostMapping("/api/buyer/groups/{groupId}/associate/{sellerId}")
    public ResponseEntity<?> requestAssociation(
            @PathVariable Long groupId,
            @PathVariable Long sellerId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(associationService.requestAssociation(userId, groupId, sellerId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // POST /api/buyer/groups/{groupId}/associate/cancel
    // Cancel a pending association request.
    @PostMapping("/api/buyer/groups/{groupId}/associate/cancel")
    public ResponseEntity<?> cancelAssociation(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(associationService.cancelAssociation(userId, groupId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // POST /api/buyer/groups/{groupId}/disassociate
    // Request disassociation from the currently associated seller.
    @PostMapping("/api/buyer/groups/{groupId}/disassociate")
    public ResponseEntity<?> requestDisassociation(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(associationService.requestDisassociation(userId, groupId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // GET /api/buyer/groups/{groupId}/association?userId={userId}
    // Returns the current association status for the group (null if none).
    @GetMapping("/api/buyer/groups/{groupId}/association")
    public ResponseEntity<?> getGroupAssociation(
            @PathVariable Long groupId,
            @RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            Map<String, Object> result = associationService.getGroupAssociation(userId, groupId);
            return ResponseEntity.ok(result != null ? result : Map.of());
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // ── Seller-side ───────────────────────────────────────────────────────

    // GET /api/seller/associations/pending?userId={userId}
    // All pending association requests for the seller.
    @GetMapping("/api/seller/associations/pending")
    public ResponseEntity<?> getSellerPendingRequests(@RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(associationService.getSellerPendingRequests(userId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // GET /api/seller/associations?userId={userId}
    // All active associations for the seller (ASSOCIATED + PENDING_DISASSOCIATION).
    @GetMapping("/api/seller/associations")
    public ResponseEntity<?> getSellerAssociations(@RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(associationService.getSellerAssociations(userId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // POST /api/seller/associations/{associationId}/respond
    // Body: { "userId": 5, "action": "APPROVE"|"DENY", "note": "optional" }
    @PostMapping("/api/seller/associations/{associationId}/respond")
    public ResponseEntity<?> respondToAssociation(
            @PathVariable Long associationId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            String action = (String) body.get("action");
            String note = body.containsKey("note") ? (String) body.get("note") : null;
            return ResponseEntity.ok(associationService.respondToAssociation(userId, associationId, action, note));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // POST /api/seller/associations/{associationId}/respond-disassociation
    // Body: { "userId": 5, "action": "CONFIRM"|"DENY" }
    @PostMapping("/api/seller/associations/{associationId}/respond-disassociation")
    public ResponseEntity<?> respondToDisassociation(
            @PathVariable Long associationId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            String action = (String) body.get("action");
            return ResponseEntity.ok(associationService.respondToDisassociation(userId, associationId, action));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // GET /api/seller/associations/{associationId}/messages?userId={userId}
    // Returns the last 50 chat messages for the associated group.
    // Verifies the seller owns this association and it is currently active.
    @GetMapping("/api/seller/associations/{associationId}/messages")
    public ResponseEntity<?> getGroupMessages(
            @PathVariable Long associationId,
            @RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

            var seller = sellerRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            var assoc = associationRepository.findById(associationId)
                    .orElseThrow(() -> new RuntimeException("Association not found"));

            if (!assoc.getSeller().getId().equals(seller.getId()))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

            if (assoc.getStatus() != AssociationStatus.ASSOCIATED &&
                assoc.getStatus() != AssociationStatus.PENDING_DISASSOCIATION)
                return ResponseEntity.status(403).body(Map.of("error", "Not an active association"));

            Long groupId = assoc.getGroup().getId();
            var messages = groupMessageRepository.findTop50ByGroupIdOrderBySentAtAsc(groupId);
            var result = messages.stream().map(m -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", m.getId());
                entry.put("senderId", m.getSender().getId());
                entry.put("senderName", m.getSender().getFirstName());
                entry.put("senderRole", m.getSender().getId().equals(seller.getUser().getId()) ? "SELLER" : "BUYER");
                entry.put("content", m.getContent());
                entry.put("sentAt", m.getSentAt().toString());
                return entry;
            }).toList();

            return ResponseEntity.ok(Map.of("groupId", groupId, "messages", result));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }
}
