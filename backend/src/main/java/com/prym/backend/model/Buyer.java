package com.prym.backend.model;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;
// Represents buyer-specific data (meat preferences). Personal info like name and phone are stored in User.
@Entity
@Table(name = "buyers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Buyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links this buyer profile to their login account. A user must exist before a buyer profile can be created.
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    //comma separated list of cut names the buyer prefers, like "Chuck, Rib"
    private String preferredCuts;

    //list of farms this buyer has bookmarked/saved for quick access
    @ManyToMany()
    @JoinTable(name="saved_farms", joinColumns = @JoinColumn(name = "buyer_id"), inverseJoinColumns = @JoinColumn(name = "seller_id"))
    private List<Seller> savedFarms = new ArrayList<>();

    //helper to add a farm to the saved list
    public void saveFarm(Seller farm) {
    	savedFarms.add(farm);
    }

    //helper to unsave a farm, pretty straightforward
    public void removeSavedFarm(Seller farm) {
    	savedFarms.remove(farm);
    }
}
