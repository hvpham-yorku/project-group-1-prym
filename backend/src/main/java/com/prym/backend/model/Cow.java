package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

// Represents a specific individual cow of a given CowType on a seller's farm.
// When created, 22 CowCut records are auto-generated (11 cuts × 2 sides).
// Multiple buyers are matched to one cow through BuyerMatch records.
@Entity
@Table(name = "cows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cow_type_id", nullable = false)
    private CowType cowType;

    private String name;                  // Optional label, e.g. "Lot #4"
    private Double estimatedWeightLbs;    // Hanging weight estimate
    private LocalDate harvestDate;        // When this cow will be processed

    public enum CowStatus {
        OPEN,       // Accepting buyer matches — cuts still available
        MATCHED,    // All cuts claimed by buyers
        PROCESSING  // Cow is being butchered/processed
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CowStatus status = CowStatus.OPEN;

    // All 22 cut records for this cow (11 cuts × LEFT + RIGHT sides)
    @OneToMany(mappedBy = "cow", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<CowCut> cuts;

    // One entry per buyer who has been matched to share this cow
    @OneToMany(mappedBy = "cow", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<BuyerMatch> matches;
}
