package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

// Represents seller-specific data. Each seller IS a farm.
// Personal info like name, email, and password are stored in User.
@Entity
@Table(name = "sellers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links this seller profile to their login account. A user must exist before a seller profile can be created.
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String shopName;       // Name of the farm/shop
    private String shopAddress;    // Address of the farm
    private String description;    // Description of the farm and its practices
    @Column(nullable = false)
    private double averageRating = 0.0;

    @Column(nullable = false)
    private int totalRatings = 0;

    // A seller can hold multiple certifications (e.g. Organic + Halal at the same time)
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Certification> certifications;
}