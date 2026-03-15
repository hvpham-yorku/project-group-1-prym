package com.prym.backend.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//Handles all business logic for the group buying feature (Model 2).
// Groups are standalone entities — not tied to any specific seller cow.
// Each group has a user-defined name, certifications, and members who
//independently claim cuts of a generic virtual cow.
@Service
public class GroupService {

    private final BuyerGroupRepository groupRepository;
    private final BuyerGroupMemberRepository memberRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;

    private static final int MAX_QTY_PER_CUT = 2;
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    public GroupService(
            BuyerGroupRepository groupRepository,
            BuyerGroupMemberRepository memberRepository,
            BuyerRepository buyerRepository,
            SellerRepository sellerRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.buyerRepository = buyerRepository;
        this.sellerRepository = sellerRepository;
    }

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

    //Sums claimed qty per cut across a list of members.
    private Map<String, Integer> computeTotalQty(List<BuyerGroupMember> members) {
        Map<String, Integer> total = new LinkedHashMap<>();
        for (BuyerGroupMember m : members) {
            parseCuts(m.getClaimedCuts()).forEach((cut, qty) ->
                    total.merge(cut, qty, Integer::sum));
        }
        return total;
    }

//Group builder

    private Map<String, Object> buildGroupDTO(BuyerGroup group, Long buyerUserId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        List<BuyerGroupMember> allMembers = memberRepository.findByGroupId(group.getId());

        boolean alreadyJoined = allMembers.stream()
                .anyMatch(m -> m.getBuyer().getId().equals(buyer.getId()));

        boolean isCreator = group.getCreator().getId().equals(buyer.getId());

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

        //Parse certifications string → list
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
        dto.put("isCreator", isCreator);
        dto.put("myClaimedCuts", myClaimedCuts);
        dto.put("othersClaimedQty", othersClaimedQty);
        // Only expose the invite code to current members
        if (alreadyJoined) {
            dto.put("inviteCode", group.getInviteCode());
        }

        return dto;
    }

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
        group.setInviteCode(generateInviteCode());
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

    // Returns perfect and partial farm matches based on the group's certifications.
    @Transactional(readOnly = true)
    public Map<String, Object> getMatchingFarms(Long buyerUserId, Long groupId) {
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Parse group's required certifications into a set of uppercase strings
        Set<String> required = new HashSet<>();
        if (group.getCertifications() != null && !group.getCertifications().isBlank()) {
            for (String c : group.getCertifications().split(",")) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty()) required.add(trimmed.toUpperCase());
            }
        }
        int totalRequired = required.size();

        List<Map<String, Object>> perfectMatches = new ArrayList<>();
        List<Map<String, Object>> partialMatches = new ArrayList<>();

        for (Seller seller : sellerRepository.findAll()) {
            // Collect seller's cert names
            Set<String> sellerCerts = new HashSet<>();
            if (seller.getCertifications() != null) {
                for (com.prym.backend.model.Certification cert : seller.getCertifications()) {
                    if (cert.getName() != null) {
                        sellerCerts.add(cert.getName().name());
                    }
                }
            }

            int matchCount = 0;
            for (String r : required) {
                if (sellerCerts.contains(r)) matchCount++;
            }

            // Skip sellers with no overlap when group has certifications
            if (totalRequired > 0 && matchCount == 0) continue;

            Map<String, Object> farmDTO = new LinkedHashMap<>();
            farmDTO.put("sellerId", seller.getId());
            farmDTO.put("shopName", seller.getShopName());
            farmDTO.put("sellerName", seller.getUser().getFirstName() + " " + seller.getUser().getLastName());
            farmDTO.put("email", seller.getUser().getEmail());
            farmDTO.put("phoneNumber", seller.getUser().getPhoneNumber());
            farmDTO.put("certifications", new ArrayList<>(sellerCerts));
            farmDTO.put("matchCount", matchCount);
            farmDTO.put("totalRequired", totalRequired);

            if (totalRequired == 0 || matchCount == totalRequired) {
                perfectMatches.add(farmDTO);
            } else {
                partialMatches.add(farmDTO);
            }
        }

        // Sort partial matches by matchCount descending
        partialMatches.sort((a, b) -> Integer.compare((int) b.get("matchCount"), (int) a.get("matchCount")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("perfectMatches", perfectMatches);
        result.put("partialMatches", partialMatches);
        return result;
    }

    // Looks up a group by its invite code. Returns the full group DTO for preview.
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupByCode(Long buyerUserId, String code) {
        BuyerGroup group = groupRepository.findByInviteCode(code.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("No group found with that code."));
        return buildGroupDTO(group, buyerUserId);
    }

    // Generates a new invite code for the group. Only the current creator can do this.
    @Transactional
    public Map<String, Object> regenerateInviteCode(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if (!group.getCreator().getId().equals(buyer.getId())) {
            throw new RuntimeException("Only the group creator can regenerate the invite code.");
        }
        group.setInviteCode(generateInviteCode());
        groupRepository.save(group);
        return buildGroupDTO(group, buyerUserId);
    }

    // Removes the buyer from the group. If the group has no members left, it is deleted.
    @Transactional
    public void leaveGroup(Long buyerUserId, Long groupId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        BuyerGroupMember membership = memberRepository
                .findByGroupIdAndBuyerId(groupId, buyer.getId())
                .orElseThrow(() -> new RuntimeException("You are not a member of this group."));

        boolean wasCreator = group.getCreator().getId().equals(buyer.getId());

        memberRepository.delete(membership);
        memberRepository.flush();

        List<BuyerGroupMember> remaining = memberRepository.findByGroupId(groupId);
        if (remaining.isEmpty()) {
            groupRepository.deleteById(groupId);
        } else if (wasCreator) {
            // Transfer creator to the earliest-joined remaining member (lowest ID = joined first)
            BuyerGroupMember next = remaining.stream()
                    .min(Comparator.comparing(BuyerGroupMember::getId))
                    .orElseThrow();
            group.setCreator(next.getBuyer());
            groupRepository.save(group);
        }
    }
}
