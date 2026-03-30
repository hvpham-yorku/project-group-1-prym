package com.prym.backend.controller;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Handles STOMP WebSocket messages for group chat.
// Clients send to /app/chat/{groupId} and receive from /topic/group/{groupId}.
// Authorized senders: group members (buyers) OR the group's associated seller.
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageRepository groupMessageRepository;
    private final BuyerGroupRepository buyerGroupRepository;
    private final BuyerGroupMemberRepository buyerGroupMemberRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final GroupSellerAssociationRepository associationRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          GroupMessageRepository groupMessageRepository,
                          BuyerGroupRepository buyerGroupRepository,
                          BuyerGroupMemberRepository buyerGroupMemberRepository,
                          BuyerRepository buyerRepository,
                          SellerRepository sellerRepository,
                          UserRepository userRepository,
                          GroupSellerAssociationRepository associationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.groupMessageRepository = groupMessageRepository;
        this.buyerGroupRepository = buyerGroupRepository;
        this.buyerGroupMemberRepository = buyerGroupMemberRepository;
        this.buyerRepository = buyerRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.associationRepository = associationRepository;
    }

    // Client sends: { "content": "hello" } to /app/chat/{groupId}
    // Server broadcasts the saved message to /topic/group/{groupId}
    @MessageMapping("/chat/{groupId}")
    public void sendMessage(@DestinationVariable Long groupId,
                            @Payload Map<String, String> payload,
                            Principal principal) {
        if (principal == null) return;

        User sender = userRepository.findByEmail(principal.getName()).orElse(null);
        if (sender == null) return;

        BuyerGroup group = buyerGroupRepository.findById(groupId).orElse(null);
        if (group == null) return;

        String content = payload.get("content");
        if (content == null || content.isBlank()) return;
        content = content.trim();
        if (content.length() > 1000) return;

        // Determine if sender is an authorized buyer member
        String senderRole;
        Buyer buyer = buyerRepository.findByUserId(sender.getId()).orElse(null);
        if (buyer != null && buyerGroupMemberRepository.existsByGroupIdAndBuyerId(groupId, buyer.getId())) {
            senderRole = "BUYER";
        } else {
            // Check if sender is the group's associated seller (while association is active)
            Seller seller = sellerRepository.findByUserId(sender.getId()).orElse(null);
            if (seller == null) return;

            boolean isAssociatedSeller = associationRepository
                    .findByGroupIdAndStatusIn(groupId,
                            List.of(AssociationStatus.ASSOCIATED, AssociationStatus.PENDING_DISASSOCIATION))
                    .map(a -> a.getSeller().getId().equals(seller.getId()))
                    .orElse(false);

            if (!isAssociatedSeller) return;
            senderRole = "SELLER";
        }

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        groupMessageRepository.save(message);

        // Broadcast to all subscribers of this group's topic
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", message.getId());
        response.put("senderId", sender.getId());
        response.put("senderName", sender.getFirstName());
        response.put("senderRole", senderRole);
        response.put("content", message.getContent());
        response.put("sentAt", message.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        messagingTemplate.convertAndSend("/topic/group/" + groupId, (Object) response);
    }
}
