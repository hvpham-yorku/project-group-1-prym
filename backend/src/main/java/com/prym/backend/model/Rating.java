package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//A rating that a buyer leaves for a seller/farm after a transaction.
//Buyers need a one-time code from the seller to be able to submit one of these.
@Entity
@Table(name = "ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //the farm/seller being rated
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    //the buyer who submitted this rating
    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    //score value, like 1-5 stars or whatever we decide on
    private int score;
    private LocalDateTime createdAt;

    //auto-stamps when this rating was created
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
