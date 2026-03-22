package com.prym.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
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

    private String preferredCuts;
    
    @OneToMany()
    @JoinColumn(name="buyer_id", referencedColumnName="id")
    private List<Seller> savedFarms;
    
    public void saveFarm(Seller farm) {
    	savedFarms.add(farm);
    }
    
    public void removeSavedFarm(Seller farm) {
    	savedFarms.remove(farm);
    }
}
