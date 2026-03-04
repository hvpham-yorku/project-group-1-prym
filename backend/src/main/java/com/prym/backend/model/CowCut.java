package com.prym.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Represents one side (LEFT or RIGHT) of a single cut from a specific cow.
// Every Cow has 22 CowCut records auto-generated on creation: 11 cuts × 2 sides.
// A buyer can claim one side (quantity 1) or both sides (quantity 2) of a cut.
@Entity
@Table(name = "cow_cuts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CowCut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cow_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Cow cow;

    public enum CutName {
        CHUCK,
        NECK,
        RIB,
        SHORT_LOIN,
        SIRLOIN,
        ROUND,
        BRISKET,
        PLATE,
        FLANK,
        SHANK_FRONT,
        SHANK_REAR
    }

    public enum Side {
        LEFT,
        RIGHT
    }

    public enum CutStatus {
        AVAILABLE,
        CLAIMED
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CutName cutName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CutStatus status = CutStatus.AVAILABLE;

    // Set when this cut is claimed by a buyer match. Null when still available.
    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonIgnore
    @ToString.Exclude
    private BuyerMatch match;
}
