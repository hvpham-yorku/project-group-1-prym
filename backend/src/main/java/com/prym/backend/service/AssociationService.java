package com.prym.backend.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssociationService {

    private static final List<AssociationStatus> ACTIVE_STATUSES = List.of(
            AssociationStatus.PENDING_ASSOCIATION,
            AssociationStatus.ASSOCIATED,
            AssociationStatus.PENDING_DISASSOCIATION
    );

    private final GroupSellerAssociationRepository associationRepository;
    private final BuyerGroupRepository groupRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final BuyerGroupMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AssociationService(GroupSellerAssociationRepository associationRepository,
                              BuyerGroupRepository groupRepository,
                              BuyerRepository buyerRepository,
                              SellerRepository sellerRepository,
                              BuyerGroupMemberRepository memberRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.associationRepository = associationRepository;
        this.groupRepository = groupRepository;
        this.buyerRepository = buyerRepository;
        this.sellerRepository = sellerRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ── Group-side operations ──────────────────────────────────────────────

    // Sends an association request to a seller. Only the group creator can do this.
    // Enforces: one active request at a time, 24-hr lockdown after denial.
    @Transactional
    public Map<String, Object> requestAssociation(Long buyerUserId, Long groupId, Long sellerId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (!group.getCreator().getId().equals(buyer.getId())) {
            throw new RuntimeException("Only the group creator can request association.");
        }

        // Block if group already has an active request/association (locked to prevent race)
        Optional<GroupSellerAssociation> existing = associationRepository
                .findByGroupIdAndStatusInForUpdate(groupId, ACTIVE_STATUSES);
        if (existing.isPresent()) {
            throw new RuntimeException("Group already has a pending or active association.");
        }

        // 24-hr lockdown check for this specific seller
        Optional<GroupSellerAssociation> lastRecord = associationRepository
                .findTopByGroupIdAndSellerIdOrderByRequestedAtDesc(groupId, sellerId);
        if (lastRecord.isPresent() && lastRecord.get().getStatus() == AssociationStatus.DENIED) {
            LocalDateTime lockoutEnds = lastRecord.get().getDeniedAt().plusHours(24);
            if (LocalDateTime.now().isBefore(lockoutEnds)) {
                throw new RuntimeException(
                        "You must wait 24 hours after a denial before requesting this seller again.");
            }
        }

        GroupSellerAssociation assoc = new GroupSellerAssociation();
        assoc.setGroup(group);
        assoc.setSeller(seller);
        assoc.setStatus(AssociationStatus.PENDING_ASSOCIATION);
        assoc.setRequestedAt(LocalDateTime.now());
        GroupSellerAssociation saved = associationRepository.save(assoc);

        // Real-time notification to seller
        messagingTemplate.convertAndSend(
                "/topic/seller/" + seller.getId() + "/notifications",
                (Object) Map.of(
                        "type", "ASSOCIATION_REQUEST",
                        "associationId", saved.getId(),
                        "groupId", group.getId(),
                        "groupName", group.getName()
                )
        );

        return buildAssociationDTO(saved);
    }

    // Cancels a pending association request. Only the group creator can do this.
    @Transactional
    public Map<String, Object> cancelAssociation(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreator().getId().equals(buyer.getId())) {
            throw new RuntimeException("Only the group creator can cancel the association request.");
        }

        GroupSellerAssociation assoc = associationRepository
                .findByGroupIdAndStatusInForUpdate(groupId, List.of(AssociationStatus.PENDING_ASSOCIATION))
                .orElseThrow(() -> new RuntimeException("No pending association request to cancel."));

        assoc.setStatus(AssociationStatus.CANCELLED);
        associationRepository.save(assoc);

        // Notify seller the request was withdrawn
        messagingTemplate.convertAndSend(
                "/topic/seller/" + assoc.getSeller().getId() + "/notifications",
                (Object) Map.of(
                        "type", "ASSOCIATION_CANCELLED",
                        "associationId", assoc.getId(),
                        "groupId", group.getId(),
                        "groupName", group.getName()
                )
        );

        return Map.of("message", "Association request cancelled.");
    }

    // Sends a disassociation request to the seller. Only the group creator can do this.
    @Transactional
    public Map<String, Object> requestDisassociation(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreator().getId().equals(buyer.getId())) {
            throw new RuntimeException("Only the group creator can request disassociation.");
        }

        GroupSellerAssociation assoc = associationRepository
                .findByGroupIdAndStatusInForUpdate(groupId, List.of(AssociationStatus.ASSOCIATED))
                .orElseThrow(() -> new RuntimeException("Group is not currently associated with a seller."));

        assoc.setStatus(AssociationStatus.PENDING_DISASSOCIATION);
        assoc.setDisassociationRequestedAt(LocalDateTime.now());
        associationRepository.save(assoc);

        // Notify seller
        messagingTemplate.convertAndSend(
                "/topic/seller/" + assoc.getSeller().getId() + "/notifications",
                (Object) Map.of(
                        "type", "DISASSOCIATION_REQUEST",
                        "associationId", assoc.getId(),
                        "groupId", group.getId(),
                        "groupName", group.getName()
                )
        );

        return buildAssociationDTO(assoc);
    }

    // Returns the current (non-terminal) association status for a group, or null if none.
    // Requires the requesting user to be a member of the group.
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupAssociation(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        boolean isMember = memberRepository.existsByGroupIdAndBuyerId(groupId, buyer.getId());
        if (!isMember) throw new RuntimeException("Access denied");
        return associationRepository
                .findByGroupIdAndStatusIn(groupId, ACTIVE_STATUSES)
                .map(this::buildAssociationDTO)
                .orElse(null);
    }

    // ── Seller-side operations ─────────────────────────────────────────────

    // All pending association requests for a seller (with full group + member details).
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSellerPendingRequests(Long sellerUserId) {
        Seller seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        return associationRepository
                .findBySellerIdAndStatus(seller.getId(), AssociationStatus.PENDING_ASSOCIATION)
                .stream()
                .map(this::buildDetailedAssociationDTO)
                .collect(Collectors.toList());
    }

    // All active associations for a seller (ASSOCIATED + PENDING_DISASSOCIATION).
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSellerAssociations(Long sellerUserId) {
        Seller seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        return associationRepository
                .findBySellerIdAndStatusIn(seller.getId(),
                        List.of(AssociationStatus.ASSOCIATED, AssociationStatus.PENDING_DISASSOCIATION))
                .stream()
                .map(this::buildDetailedAssociationDTO)
                .collect(Collectors.toList());
    }

    // Seller approves or denies an association request.
    @Transactional
    public Map<String, Object> respondToAssociation(Long sellerUserId, Long associationId,
                                                     String action, String note) {
        Seller seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        GroupSellerAssociation assoc = associationRepository.findById(associationId)
                .orElseThrow(() -> new RuntimeException("Association not found"));

        if (!assoc.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied.");
        }
        if (assoc.getStatus() != AssociationStatus.PENDING_ASSOCIATION) {
            throw new RuntimeException("This request is no longer pending.");
        }

        assoc.setSellerNote(note);
        assoc.setRespondedAt(LocalDateTime.now());

        if ("APPROVE".equalsIgnoreCase(action)) {
            assoc.setStatus(AssociationStatus.ASSOCIATED);
            // Update the group's denormalized associatedSeller field
            BuyerGroup group = assoc.getGroup();
            group.setAssociatedSeller(seller);
            groupRepository.save(group);

            notifyGroup(assoc.getGroup().getId(), Map.of(
                    "type", "ASSOCIATION_APPROVED",
                    "associationId", assoc.getId(),
                    "sellerId", seller.getId(),
                    "shopName", seller.getShopName() != null ? seller.getShopName() : "",
                    "sellerName", seller.getUser().getFirstName() + " " + seller.getUser().getLastName()
            ));
        } else if ("DENY".equalsIgnoreCase(action)) {
            assoc.setStatus(AssociationStatus.DENIED);
            assoc.setDeniedAt(LocalDateTime.now());

            notifyGroup(assoc.getGroup().getId(), Map.of(
                    "type", "ASSOCIATION_DENIED",
                    "associationId", assoc.getId(),
                    "note", note != null ? note : ""
            ));
        } else {
            throw new RuntimeException("Invalid action. Use APPROVE or DENY.");
        }

        associationRepository.save(assoc);
        return buildAssociationDTO(assoc);
    }

    // Seller confirms or denies the group's disassociation request.
    @Transactional
    public Map<String, Object> respondToDisassociation(Long sellerUserId, Long associationId,
                                                        String action) {
        Seller seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        GroupSellerAssociation assoc = associationRepository.findById(associationId)
                .orElseThrow(() -> new RuntimeException("Association not found"));

        if (!assoc.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied.");
        }
        if (assoc.getStatus() != AssociationStatus.PENDING_DISASSOCIATION) {
            throw new RuntimeException("No pending disassociation request for this association.");
        }

        assoc.setDisassociationRespondedAt(LocalDateTime.now());

        if ("CONFIRM".equalsIgnoreCase(action)) {
            assoc.setStatus(AssociationStatus.DISASSOCIATED);
            // Clear the denormalized field on the group
            BuyerGroup group = assoc.getGroup();
            group.setAssociatedSeller(null);
            groupRepository.save(group);

            notifyGroup(assoc.getGroup().getId(), Map.of(
                    "type", "DISASSOCIATION_CONFIRMED",
                    "associationId", assoc.getId()
            ));
        } else if ("DENY".equalsIgnoreCase(action)) {
            assoc.setStatus(AssociationStatus.ASSOCIATED);

            notifyGroup(assoc.getGroup().getId(), Map.of(
                    "type", "DISASSOCIATION_DENIED",
                    "associationId", assoc.getId()
            ));
        } else {
            throw new RuntimeException("Invalid action. Use CONFIRM or DENY.");
        }

        associationRepository.save(assoc);
        return buildAssociationDTO(assoc);
    }

    // ── DTO builders ──────────────────────────────────────────────────────

    private void notifyGroup(Long groupId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/association", (Object) payload);
    }

    private Map<String, Object> buildAssociationDTO(GroupSellerAssociation assoc) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("associationId", assoc.getId());
        dto.put("groupId", assoc.getGroup().getId());
        dto.put("sellerId", assoc.getSeller().getId());
        dto.put("shopName", assoc.getSeller().getShopName());
        dto.put("sellerName", assoc.getSeller().getUser().getFirstName()
                + " " + assoc.getSeller().getUser().getLastName());
        dto.put("status", assoc.getStatus().name());
        dto.put("requestedAt", assoc.getRequestedAt() != null ? assoc.getRequestedAt().toString() : null);
        dto.put("deniedAt", assoc.getDeniedAt() != null ? assoc.getDeniedAt().toString() : null);
        dto.put("sellerNote", assoc.getSellerNote());
        return dto;
    }

    // Includes full group details and members — used for seller's request inbox
    private Map<String, Object> buildDetailedAssociationDTO(GroupSellerAssociation assoc) {
        Map<String, Object> dto = buildAssociationDTO(assoc);

        BuyerGroup group = assoc.getGroup();
        List<BuyerGroupMember> members = memberRepository.findByGroupId(group.getId());

        List<String> certs = new ArrayList<>();
        if (group.getCertifications() != null && !group.getCertifications().isBlank()) {
            certs = Arrays.asList(group.getCertifications().split(","));
        }

        List<Map<String, Object>> memberDTOs = members.stream().map(m -> {
            Map<String, Object> md = new LinkedHashMap<>();
            md.put("firstName", m.getBuyer().getUser().getFirstName());
            md.put("lastName", m.getBuyer().getUser().getLastName());
            md.put("claimedCuts", m.getClaimedCuts());
            return md;
        }).collect(Collectors.toList());

        dto.put("groupName", group.getName());
        dto.put("certifications", certs);
        dto.put("memberCount", members.size());
        dto.put("members", memberDTOs);
        dto.put("disassociationRequestedAt",
                assoc.getDisassociationRequestedAt() != null
                        ? assoc.getDisassociationRequestedAt().toString() : null);

        return dto;
    }
}
