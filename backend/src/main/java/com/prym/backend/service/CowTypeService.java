package com.prym.backend.service;

import com.prym.backend.model.CowType;
import com.prym.backend.model.Seller;
import com.prym.backend.repository.CowTypeRepository;
import com.prym.backend.repository.SellerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CowTypeService {

    private final CowTypeRepository cowTypeRepository;
    private final SellerRepository sellerRepository;

    public CowTypeService(CowTypeRepository cowTypeRepository, SellerRepository sellerRepository) {
        this.cowTypeRepository = cowTypeRepository;
        this.sellerRepository = sellerRepository;
    }

    public CowType createCowType(Long userId, String breed, String description, Double pricePerPound, Integer availableCount) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        CowType cowType = new CowType();
        cowType.setSeller(seller);
        cowType.setBreed(CowType.Breed.valueOf(breed));
        cowType.setDescription(description);
        cowType.setPricePerPound(pricePerPound);
        cowType.setAvailableCount(availableCount);

        return cowTypeRepository.save(cowType);
    }

    public List<CowType> getCowTypesBySeller(Long userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        return cowTypeRepository.findBySellerId(seller.getId());
    }

    public CowType updateCowType(Long cowTypeId, String breed, String description, Double pricePerPound, Integer availableCount) {
        CowType cowType = cowTypeRepository.findById(cowTypeId)
                .orElseThrow(() -> new RuntimeException("CowType not found"));

        if (breed != null) cowType.setBreed(CowType.Breed.valueOf(breed));
        if (description != null) cowType.setDescription(description);
        if (pricePerPound != null) cowType.setPricePerPound(pricePerPound);
        if (availableCount != null) cowType.setAvailableCount(availableCount);

        return cowTypeRepository.save(cowType);
    }

    public void deleteCowType(Long cowTypeId) {
        cowTypeRepository.deleteById(cowTypeId);
    }

    // Public lookup by seller profile ID (not user ID) — used by buyers viewing a farm listing
    public List<CowType> getCowTypesBySellerProfileId(Long sellerId) {
        return cowTypeRepository.findBySellerId(sellerId);
    }
}
