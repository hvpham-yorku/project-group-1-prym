package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Represents one buyer's membership in a BuyerGroup.
// claimedCuts is null until the buyer saves their cut selections on the group page.
@Entity
@Table(name = "buyer_group_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "buyer_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Exclude
    private BuyerGroup group;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    // Serialized cut string, e.g. "Chuck, Rib x2" — null until member saves cuts
    @Column
    private String claimedCuts;
}
