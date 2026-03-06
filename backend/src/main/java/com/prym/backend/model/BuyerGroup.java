package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Represents a buyer-created group for collectively purchasing a cow.
// The group is independent of any specific seller cow — it is a virtual
// arrangement where buyers divide cuts of a generic cow.
@Entity
@Table(name = "buyer_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User-defined name for the group, displayed on the group page
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private Buyer creator;

    // Comma-separated certification enum names, e.g. "KOSHER,ORGANIC"
    @Column
    private String certifications;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<BuyerGroupMember> members = new ArrayList<>();
}
