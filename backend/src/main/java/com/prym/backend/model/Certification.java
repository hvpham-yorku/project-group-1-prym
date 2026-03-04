package com.prym.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

// Represents a food/farming certification held by a seller (e.g. Halal, Organic).
// A seller can hold multiple certifications.
@Entity
@Table(name = "certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Seller seller;

    public enum CertificationType {
        HALAL,
        KOSHER,
        ORGANIC,
        GRASS_FED,
        NON_GMO,
        ANIMAL_WELFARE_APPROVED,
        CONVENTIONAL
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificationType name;

    private String issuingBody;   // e.g. "USDA", "PCO"
    private LocalDate expiryDate;
}
