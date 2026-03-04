package com.prym.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Represents a general type/breed of cow that a seller offers.
// One seller can list multiple cow types (e.g. Wagyu and Angus).
// Specific individual cows of this type are represented by the Cow entity.
@Entity
@Table(name = "cow_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CowType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Seller seller;

    public enum Breed {
        WAGYU,
        ANGUS,
        GRASS_FED,
        HERITAGE,
        CONVENTIONAL
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Breed breed;

    private String description;       // e.g. "Pasture-raised, hormone-free"
    private Double pricePerPound;     // Base price per pound (hanging weight)
    private Integer availableCount;   // How many cows of this type the seller currently has
}
