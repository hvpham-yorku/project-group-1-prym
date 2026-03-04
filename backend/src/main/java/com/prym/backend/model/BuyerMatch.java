package com.prym.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

// Represents one buyer's participation in a group share of a specific cow.
// Multiple BuyerMatch records per Cow — one per buyer in the group.
// The CowCut records assigned to this match are the buyer's claimed cuts.
@Entity
@Table(name = "buyer_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cow_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Cow cow;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    public enum MatchStatus {
        PENDING,      // Buyer expressed interest, awaiting full group formation
        CONFIRMED,    // Full group formed, all parties confirmed
        COMPLETED     // Cow processed and order fulfilled
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    // The specific CowCut records (left/right sides of cuts) assigned to this buyer
    @OneToMany(mappedBy = "match")
    @ToString.Exclude
    private List<CowCut> claimedCuts;
}
