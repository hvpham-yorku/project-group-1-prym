package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Tracks the lifecycle of a group-seller association request.
// One record acts as both the request and the association — status drives the state machine.
// A group can only have one non-terminal record at a time (enforced in service layer).
// Terminal statuses: DISASSOCIATED, DENIED, CANCELLED.
@Entity
@Table(name = "group_seller_associations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSellerAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private BuyerGroup group;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssociationStatus status;

    // Set when group sends the association request
    @Column(nullable = false)
    private LocalDateTime requestedAt;

    // Set when seller approves or denies the association request
    private LocalDateTime respondedAt;

    // Set when status transitions to DENIED — used for the 24-hour re-request lockdown
    private LocalDateTime deniedAt;

    // Set when group sends the disassociation request (status → PENDING_DISASSOCIATION)
    private LocalDateTime disassociationRequestedAt;

    // Set when seller responds to the disassociation request
    private LocalDateTime disassociationRespondedAt;

    // Optional message from seller when denying or approving
    @Column(length = 500)
    private String sellerNote;
}
