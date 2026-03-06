package com.prym.backend.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Handles all business logic for the group buying feature (Model 2).
// Groups are standalone entities — not tied to any specific seller cow.
// Each group has a user-defined name, certifications, and members who
// independently claim cuts of a generic virtual cow.
@Service
public class GroupService {

    private final BuyerGroupRepository groupRepository;
    private final BuyerGroupMemberRepository memberRepository;
    private final BuyerRepository buyerRepository;

    // Each of the 11 cuts has 2 slots (left/right). Max claimable qty per cut = 2.
    private static final int MAX_QTY_PER_CUT = 2;

    public GroupService(
            BuyerGroupRepository groupRepository,
            BuyerGroupMemberRepository memberRepository,
            BuyerRepository buyerRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.buyerRepository = buyerRepository;
    }

    // ── Cut parsing ──────────────────────────────────────────────────────────────

    // "Chuck, Rib x2, Short Loin"  →  { "Chuck": 1, "Rib": 2, "Short Loin": 1 }
    private Map<String, Integer> parseCuts(String cutsStr) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (cutsStr == null || cutsStr.isBlank()) return result;
        Pattern pattern = Pattern.compile("^(.+?) x(\\d+)$");
        for (String item : cutsStr.split(", ")) {
            item = item.trim();
            if (item.isEmpty()) continue;
            Matcher m = pattern.matcher(item);
            if (m.matches()) {
                result.put(m.group(1), Integer.parseInt(m.group(2)));
            } else {
                result.put(item, 1);
            }
        }
        return result;
    }

    // Sums claimed qty per cut across a list of members.
    private Map<String, Integer> computeTotalQty(List<BuyerGroupMember> members) {
        Map<String, Integer> total = new LinkedHashMap<>();
        for (BuyerGroupMember m : members) {
            parseCuts(m.getClaimedCuts()).forEach((cut, qty) ->
                    total.merge(cut, qty, Integer::sum));
        }
        return total;
    }

    // ── DTO builder ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildGroupDTO(BuyerGroup group, Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        List<BuyerGroupMember> allMembers = memberRepository.findByGroupId(group.getId());

        boolean alreadyJoined = allMembers.stream()
                .anyMatch(m -> m.getBuyer().getId().equals(buyer.getId()));

        String myClaimedCuts = null;
        if (alreadyJoined) {
            myClaimedCuts = allMembers.stream()
                    .filter(m -> m.getBuyer().getId().equals(buyer.getId()))
                    .findFirst()
                    .map(BuyerGroupMember::getClaimedCuts)
                    .orElse(null);
        }

        // Others = all members except the requesting buyer (or all members if not joined)
        List<BuyerGroupMember> others = allMembers.stream()
                .filter(m -> !m.getBuyer().getId().equals(buyer.getId()))
                .collect(Collectors.toList());

        // Per-cut quantity claimed by others — used by the frontend to disable/limit selections
        Map<String, Integer> othersClaimedQty = computeTotalQty(others);

        // Member DTOs (visible to all group members)
        List<Map<String, Object>> memberDTOs = allMembers.stream().map(m -> {
            Map<String, Object> md = new LinkedHashMap<>();
            md.put("firstName", m.getBuyer().getUser().getFirstName());
            md.put("claimedCuts", m.getClaimedCuts());
            return md;
        }).collect(Collectors.toList());

        // Parse certifications string → list
        List<String> certs = new ArrayList<>();
        if (group.getCertifications() != null && !group.getCertifications().isBlank()) {
            certs = Arrays.asList(group.getCertifications().split(","));
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("groupId", group.getId());
        dto.put("groupName", group.getName());
        dto.put("certifications", certs);
        dto.put("memberCount", allMembers.size());
        dto.put("members", memberDTOs);
        dto.put("alreadyJoined", alreadyJoined);
        dto.put("myClaimedCuts", myClaimedCuts);
        dto.put("othersClaimedQty", othersClaimedQty);

        return dto;
    }

    // ── Public methods ───────────────────────────────────────────────────────────

    // Creates a new group and adds the creator as the first member (no cuts yet).
    @Transactional
    public Map<String, Object> createGroup(Long buyerUserId, String name, String certifications) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        if (!memberRepository.findByBuyerId(buyer.getId()).isEmpty()) {
            throw new RuntimeException("You are already in a group. Leave your current group before creating a new one.");
        }
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Group name is required.");
        }

        BuyerGroup group = new BuyerGroup();
        group.setName(name.trim());
        group.setCreator(buyer);
        group.setCertifications(certifications != null ? certifications.trim() : "");
        group.setCreatedAt(LocalDateTime.now());
        BuyerGroup saved = groupRepository.save(group);

        BuyerGroupMember member = new BuyerGroupMember();
        member.setGroup(saved);
        member.setBuyer(buyer);
        member.setClaimedCuts(null);
        memberRepository.save(member);

        return buildGroupDTO(saved, buyerUserId);
    }

    // All groups the requesting buyer has NOT yet joined.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableGroups(Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        Set<Long> myGroupIds = memberRepository.findByBuyerId(buyer.getId()).stream()
                .map(m -> m.getGroup().getId())
                .collect(Collectors.toSet());

        return groupRepository.findAll().stream()
                .filter(g -> !myGroupIds.contains(g.getId()))
                .map(g -> buildGroupDTO(g, buyerUserId))
                .collect(Collectors.toList());
    }

    // Groups the buyer is currently a member of.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyGroups(Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        return memberRepository.findByBuyerId(buyer.getId()).stream()
                .map(m -> buildGroupDTO(m.getGroup(), buyerUserId))
                .collect(Collectors.toList());
    }

    // Single group detail DTO — used by the group detail page.
    @Transactional(readOnly = true)
    public Map<String, Object> getGroup(Long buyerUserId, Long groupId) {
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return buildGroupDTO(group, buyerUserId);
    }

    // Adds the buyer as a member of an existing group (no cuts required at this point).
    @Transactional
    public Map<String, Object> joinGroup(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!memberRepository.findByBuyerId(buyer.getId()).isEmpty()) {
            throw new RuntimeException("You are already in a group. Leave your current group before joining another.");
        }
        if (memberRepository.existsByGroupIdAndBuyerId(groupId, buyer.getId())) {
            throw new RuntimeException("You are already a member of this group.");
        }

        BuyerGroupMember member = new BuyerGroupMember();
        member.setGroup(group);
        member.setBuyer(buyer);
        member.setClaimedCuts(null);
        memberRepository.save(member);

        return buildGroupDTO(group, buyerUserId);
    }

    // Saves (or updates) the buyer's claimed cuts on a group they have already joined.
    // Validates that the requested cuts don't conflict with what other members have already claimed.
    // Each cut has MAX_QTY_PER_CUT (2) total slots. A null/blank cuts string clears the member's selection.
    @Transactional
    public Map<String, Object> saveCuts(Long buyerUserId, Long groupId, String cuts) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroupMember membership = memberRepository
                .findByGroupIdAndBuyerId(groupId, buyer.getId())
                .orElseThrow(() -> new RuntimeException("You are not a member of this group."));

        Map<String, Integer> desired = parseCuts(cuts);

        // Compute what everyone else has claimed for each cut
        Map<String, Integer> othersTotal = computeTotalQty(
                memberRepository.findByGroupId(groupId).stream()
                        .filter(m -> !m.getBuyer().getId().equals(buyer.getId()))
                        .collect(Collectors.toList())
        );

        // Validate requested quantities against remaining slots
        for (Map.Entry<String, Integer> entry : desired.entrySet()) {
            String cut = entry.getKey();
            int requested = entry.getValue();
            if (requested < 1 || requested > MAX_QTY_PER_CUT) {
                throw new RuntimeException("Quantity for '" + cut + "' must be 1 or 2.");
            }
            int othersQty = othersTotal.getOrDefault(cut, 0);
            if (othersQty + requested > MAX_QTY_PER_CUT) {
                throw new RuntimeException(
                        "'" + cut + "' only has " + (MAX_QTY_PER_CUT - othersQty) + " slot(s) left.");
            }
        }

        membership.setClaimedCuts((cuts == null || cuts.isBlank()) ? null : cuts.trim());
        memberRepository.save(membership);

        BuyerGroup group = groupRepository.findById(groupId).orElseThrow();
        return buildGroupDTO(group, buyerUserId);
    }

    // Removes the buyer from the group. If the group has no members left, it is deleted.
    @Transactional
    public void leaveGroup(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroupMember membership = memberRepository
                .findByGroupIdAndBuyerId(groupId, buyer.getId())
                .orElseThrow(() -> new RuntimeException("You are not a member of this group."));

        memberRepository.delete(membership);

        // Flush so the count check below reflects the deletion
        memberRepository.flush();

        if (memberRepository.findByGroupId(groupId).isEmpty()) {
            groupRepository.deleteById(groupId);
        }
    }
}
