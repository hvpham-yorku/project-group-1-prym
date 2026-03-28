package com.prym.backend.controller;

import com.prym.backend.model.AssociationStatus;
import com.prym.backend.model.GroupMessage;
import com.prym.backend.model.User;
import com.prym.backend.repository.GroupMessageRepository;
import com.prym.backend.repository.GroupSellerAssociationRepository;
import com.prym.backend.repository.SellerRepository;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Handles HTTP requests for the group buying feature (Model 2).
// All routes live under /api/buyer which SecurityConfig already protects (BUYER role only).
@RestController
@RequestMapping("/api/buyer")
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupSellerAssociationRepository associationRepository;
    private final SellerRepository sellerRepository;

    public GroupController(GroupService groupService, UserRepository userRepository,
                           GroupMessageRepository groupMessageRepository,
                           GroupSellerAssociationRepository associationRepository,
                           SellerRepository sellerRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.associationRepository = associationRepository;
        this.sellerRepository = sellerRepository;
    }

    private Long getLoggedInUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // POST /api/buyer/groups/create
    // Body: { "userId": 5, "name": "My Halal Crew", "certifications": "KOSHER,ORGANIC" }
    @PostMapping("/groups/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            String name = (String) body.get("name");
            String certifications = body.containsKey("certifications")
                    ? (String) body.get("certifications") : "";
            return ResponseEntity.ok(groupService.createGroup(userId, name, certifications));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/groups?userId={userId}
    // Returns all groups the buyer has NOT yet joined.
    @GetMapping("/groups")
    public ResponseEntity<?> getAvailableGroups(@RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            List<Map<String, Object>> groups = groupService.getAvailableGroups(userId);
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/groups/mine?userId={userId}
    // Returns the group(s) the buyer is currently a member of.
    @GetMapping("/groups/mine")
    public ResponseEntity<?> getMyGroups(@RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            List<Map<String, Object>> groups = groupService.getMyGroups(userId);
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/groups/{groupId}?userId={userId}
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<?> getGroup(
            @PathVariable Long groupId,
            @RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(groupService.getGroup(userId, groupId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/buyer/groups/join/{groupId}
    // Body: { "userId": 5 }
    @PostMapping("/groups/join/{groupId}")
    public ResponseEntity<?> joinGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(groupService.joinGroup(userId, groupId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/buyer/groups/cuts/{groupId}
    // Body: { "userId": 5, "cuts": "Chuck, Rib x2" }
    // Saves (or updates) the buyer's claimed cuts. Validates against other members' selections.
    @PostMapping("/groups/cuts/{groupId}")
    public ResponseEntity<?> saveCuts(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            String cuts = body.containsKey("cuts") ? (String) body.get("cuts") : null;
            return ResponseEntity.ok(groupService.saveCuts(userId, groupId, cuts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/groups/by-code/{code}?userId={userId}
    // Looks up a group by invite code — navigates the user to the group detail page for preview.
    @GetMapping("/groups/by-code/{code}")
    public ResponseEntity<?> getGroupByCode(
            @PathVariable String code,
            @RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(groupService.getGroupByCode(userId, code));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/buyer/groups/{groupId}/regenerate-code
    // Body: { "userId": 5 }
    @PostMapping("/groups/{groupId}/regenerate-code")
    public ResponseEntity<?> regenerateInviteCode(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(groupService.regenerateInviteCode(userId, groupId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/groups/{groupId}/matching-farms?userId={userId}
    // Returns perfect and partial farm matches based on the group's certifications.
    @GetMapping("/groups/{groupId}/matching-farms")
    public ResponseEntity<?> getMatchingFarms(
            @PathVariable Long groupId,
            @RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            return ResponseEntity.ok(groupService.getMatchingFarms(userId, groupId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/buyer/groups/{groupId}/messages?userId={userId}
    // Returns the last 50 chat messages for a group, oldest first.
    // Each message includes senderRole (BUYER or SELLER) so the frontend can style them.
    @GetMapping("/groups/{groupId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long groupId,
            @RequestParam Long userId) {
        try {
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

            // Resolve the associated seller's user ID (if any) for senderRole classification
            Long associatedSellerUserId = associationRepository
                    .findByGroupIdAndStatusIn(groupId,
                            List.of(AssociationStatus.ASSOCIATED, AssociationStatus.PENDING_DISASSOCIATION))
                    .map(a -> a.getSeller().getUser().getId())
                    .orElse(null);

            var messages = groupMessageRepository.findTop50ByGroupIdOrderBySentAtAsc(groupId);
            var result = messages.stream().map(m -> {
                String senderRole = (associatedSellerUserId != null
                        && m.getSender().getId().equals(associatedSellerUserId)) ? "SELLER" : "BUYER";
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("id", m.getId());
                entry.put("senderId", m.getSender().getId());
                entry.put("senderName", m.getSender().getFirstName());
                entry.put("senderRole", senderRole);
                entry.put("content", m.getContent());
                entry.put("sentAt", m.getSentAt().toString());
                return entry;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/buyer/groups/leave/{groupId}
    // Body: { "userId": 5 }
    @PostMapping("/groups/leave/{groupId}")
    public ResponseEntity<?> leaveGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.parseLong(body.get("userId").toString());
            if (!getLoggedInUserId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            groupService.leaveGroup(userId, groupId);
            return ResponseEntity.ok(Map.of("message", "Left group successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
