package com.prym.backend.controller;

import com.prym.backend.model.Buyer;
import com.prym.backend.model.BuyerGroup;
import com.prym.backend.model.GroupMessage;
import com.prym.backend.model.User;
import com.prym.backend.repository.BuyerGroupMemberRepository;
import com.prym.backend.repository.BuyerGroupRepository;
import com.prym.backend.repository.BuyerRepository;
import com.prym.backend.repository.GroupMessageRepository;
import com.prym.backend.repository.UserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// Handles STOMP WebSocket messages for group chat.
// Clients send to /app/chat/{groupId} and receive from /topic/group/{groupId}
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageRepository groupMessageRepository;
    private final BuyerGroupRepository buyerGroupRepository;
    private final BuyerGroupMemberRepository buyerGroupMemberRepository;
    private final BuyerRepository buyerRepository;
    private final UserRepository userRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          GroupMessageRepository groupMessageRepository,
                          BuyerGroupRepository buyerGroupRepository,
                          BuyerGroupMemberRepository buyerGroupMemberRepository,
                          BuyerRepository buyerRepository,
                          UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.groupMessageRepository = groupMessageRepository;
        this.buyerGroupRepository = buyerGroupRepository;
        this.buyerGroupMemberRepository = buyerGroupMemberRepository;
        this.buyerRepository = buyerRepository;
        this.userRepository = userRepository;
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

        Buyer buyer = buyerRepository.findByUserId(sender.getId()).orElse(null);
        if (buyer == null) return;

        // Only group members can send messages
        if (!buyerGroupMemberRepository.existsByGroupIdAndBuyerId(groupId, buyer.getId())) return;

        BuyerGroup group = buyerGroupRepository.findById(groupId).orElse(null);
        if (group == null) return;

        String content = payload.get("content");
        if (content == null || content.isBlank()) return;

        GroupMessage message = new GroupMessage();
        message.setGroup(group);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setSentAt(LocalDateTime.now());
        groupMessageRepository.save(message);

        // Broadcast to all subscribers of this group's topic
        Map<String, Object> response = Map.of(
            "id", message.getId(),
            "senderId", sender.getId(),
            "senderName", sender.getFirstName(),
            "content", message.getContent(),
            "sentAt", message.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        messagingTemplate.convertAndSend("/topic/group/" + groupId, (Object) response);
    }
}
