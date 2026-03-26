package com.prym.backend.service;

import com.prym.backend.model.*;
import com.prym.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;

@Service
public class RatingService {
    private final RatingRepository ratingRepository;
    private final RatingCodeRepository ratingCodeRepository;
    private final SellerRepository sellerRepository;
    private final BuyerRepository buyerRepository;
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RatingService(RatingRepository ratingRepository, 
        RatingCodeRepository ratingCodeRepository, 
        SellerRepository sellerRepository, 
        BuyerRepository buyerRepository){
            this.ratingRepository = ratingRepository;
            this.ratingCodeRepository = ratingCodeRepository;
            this.sellerRepository = sellerRepository;
            this.buyerRepository = buyerRepository;
    }

    private String generateCode(){
        StringBuilder sb = new StringBuilder("PRYM-");
        for(int i = 0; i<6; i++){
            sb.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    //after transaction, seller generates a code
    @Transactional
    public Map<String, Object> generateRatingCode(Long sellerUserId){
        Seller seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        RatingCode ratingCode = new RatingCode();
        ratingCode.setSeller(seller);
        ratingCode.setCode(generateCode());
        ratingCode.setUsed(false);
        ratingCodeRepository.save(ratingCode);
        return Map.of("code", ratingCode.getCode());
    }

    //using the code, buyer can submit a rating
    @Transactional
    public Map<String, Object> submitRating(Long buyerUserId, String code, int score){
        Buyer buyer = buyerRepository.findByUserId(buyerUserId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        if(score < 1 || score > 5){
            throw new RuntimeException("Score must be between 1 and 5.");
        }

        String normalizedCode = code.trim().toUpperCase();

        // Atomically claim the code: only one concurrent request can win this update.
        // If 0 rows are affected, the code either doesn't exist or was already used.
        int claimed = ratingCodeRepository.markAsUsed(normalizedCode);
        if(claimed == 0){
            throw new RuntimeException("Invalid or already-used code.");
        }

        RatingCode ratingCode = ratingCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Code not found after claim."));

        // Re-fetch seller with a pessimistic write lock so concurrent submissions
        // can't produce a lost-update on averageRating / totalRatings.
        Seller seller = sellerRepository.findByIdForUpdate(ratingCode.getSeller().getId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if(ratingRepository.existsBySellerAndBuyer(seller, buyer)){
            throw new RuntimeException("You have already rated this seller.");
        }

        Rating rating = new Rating();
        rating.setSeller(seller);
        rating.setBuyer(buyer);
        rating.setScore(score);
        ratingRepository.save(rating);

        int newTotal = seller.getTotalRatings() + 1;
        double newAverage = ((seller.getAverageRating() * seller.getTotalRatings()) + score) / newTotal;
        seller.setAverageRating(newAverage);
        seller.setTotalRatings(newTotal);
        sellerRepository.save(seller);

        return Map.of("message", "Rating submitted successfully!");
    }

    //get average of ratings of a farm
    @Transactional
    public Map<String, Object> getFarmRatings(String sellerUsername){
        Seller seller = sellerRepository.findByUserUsername(sellerUsername)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        List<Rating> ratings = ratingRepository.findBySeller(seller);
        List<Map<String, Object>> ratingDTOs = ratings.stream().map(r -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("score", r.getScore());
            dto.put("createdAt", r.getCreatedAt().toString());
            return dto;
        }).toList();
        return Map.of(
            "shopName", seller.getShopName(),
            "averageRating", seller.getAverageRating(),
            "totalRatings", seller.getTotalRatings(),
            "ratings", ratingDTOs
        );
    }
}