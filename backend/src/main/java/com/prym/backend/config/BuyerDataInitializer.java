package com.prym.backend.config;

import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.BuyerService;
import com.prym.backend.repository.UserRepository;

// Creates mock buyer accounts on startup for testing purposes
@Component
public class BuyerDataInitializer implements CommandLineRunner {

    private final AuthService authService;
    private final BuyerService buyerService;
    private final UserRepository userRepository;

    public BuyerDataInitializer(AuthService authService, BuyerService buyerService, UserRepository userRepository) {
        this.authService = authService;
        this.buyerService = buyerService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Buyer Data Initializer running...");

        // Create first test buyer with a buyer profile
        if (!this.userRepository.existsByEmail("buyer1@test.com")) {
            User buyer1 = this.authService.register(
                "buyer1@test.com",
                "buyerPass1",
                User.Role.BUYER,
                "buyer1username",
                "Buyer",
                "One",
                "4165551234",
                null);
            this.buyerService.createBuyerProfile(buyer1.getId(), "Ribeye, Sirloin", "Half cow");
            System.out.println("Buyer 1 created.");
        }

        // Create second test buyer with a buyer profile
        if (!this.userRepository.existsByEmail("buyer2@test.com")) {
            User buyer2 = this.authService.register(
                "buyer2@test.com",
                "buyerPass2",
                User.Role.BUYER,
                "buyer2username",
                "Buyer",
                "Two",
                "4165555678",
                null);
            this.buyerService.createBuyerProfile(buyer2.getId(), "T-Bone, Brisket", "Quarter cow");
            System.out.println("Buyer 2 created.");
        }

        System.out.println("Mock buyer data ensured.");
    }
}
