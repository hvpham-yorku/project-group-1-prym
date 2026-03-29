package com.prym.backend.config;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.BuyerService;
import com.prym.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

//Seeds the database with test buyer accounts on startup.
//Runs at Order(2) so sellers exist first (buyers might need them for saved farms etc).
//Only creates a buyer if the email doesnt already exist so its safe to restart.
@Component
@Order(2)
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

        if (!userRepository.existsByEmail("buyer1@prym.com")) {
            User u = authService.register("buyer1@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer1", "Alice", "Smith", "4165550101", null, "M5V 3A8"); // Toronto
            buyerService.createBuyerProfile(u.getId(), "Chuck, Rib");
        }

        if (!userRepository.existsByEmail("buyer2@prym.com")) {
            User u = authService.register("buyer2@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer2", "James", "Brown", "2125550102", null, "10001"); // New York
            buyerService.createBuyerProfile(u.getId(), "Sirloin, Brisket");
        }

        if (!userRepository.existsByEmail("buyer3@prym.com")) {
            User u = authService.register("buyer3@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer3", "Sofia", "Lee", "3105550103", null, "90001"); // Los Angeles
            buyerService.createBuyerProfile(u.getId(), "Rib, Short Loin");
        }

        if (!userRepository.existsByEmail("buyer4@prym.com")) {
            User u = authService.register("buyer4@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer4", "Derek", "Khan", "3125550104", null, "60601"); // Chicago
            buyerService.createBuyerProfile(u.getId(), "Chuck, Brisket");
        }

        if (!userRepository.existsByEmail("buyer5@prym.com")) {
            User u = authService.register("buyer5@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer5", "Emma", "Torres", "6175550105", null, "02101"); // Boston
            buyerService.createBuyerProfile(u.getId(), "Sirloin, Chuck");
        }

        if (!userRepository.existsByEmail("buyer6@prym.com")) {
            User u = authService.register("buyer6@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer6", "Noah", "Patel", "7205550106", null, "80201"); // Denver
            buyerService.createBuyerProfile(u.getId(), "Chuck, Short Loin");
        }

        if (!userRepository.existsByEmail("buyer7@prym.com")) {
            User u = authService.register("buyer7@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer7", "Priya", "Singh", "2065550107", null, "98101"); // Seattle
            buyerService.createBuyerProfile(u.getId(), "Rib, Brisket");
        }

        if (!userRepository.existsByEmail("buyer8@prym.com")) {
            User u = authService.register("buyer8@prym.com", "buyerPass1", User.Role.BUYER,
                    "buyer8", "Carlos", "Rivera", "4045550108", null, "30301"); // Atlanta
            buyerService.createBuyerProfile(u.getId(), "Sirloin, Rib");
        }

        System.out.println("Buyer seed data complete.");
    }
}
