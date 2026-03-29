package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//Chat message sent inside a buyer group. Uses websockets for real-time delivery
//but we also persist them here so people can see history when they open the group.
@Entity
@Table(name = "group_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //which group this message was sent in
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BuyerGroup group;

    //who sent it, lazy loaded because we dont always need the full user object
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    //the actual message text, capped at 1000 chars so nobody writes a novel
    @Column(nullable = false, length = 1000)
    private String content;

    //when this message was sent, defaults to right now
    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
}
