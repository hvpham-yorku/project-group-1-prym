package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Represents seller-specific data. Personal info like name, email, and password are stored in User.
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

    private String shopName;                 // Name of the seller’s shop
    private String shopAddress;              // Address of the shop/store
    
    public enum SellerCategory{
    	HALAL,
    	KOSHER,
    	ORGANIC,
    	CONVENTIONAl
    }
    @Enumerated(EnumType.STRING)
    private SellerCategory category;
}
