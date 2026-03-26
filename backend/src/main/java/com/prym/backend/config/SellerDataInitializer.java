package com.prym.backend.config;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.CertificationService;
import com.prym.backend.service.SellerService;
import com.prym.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(1)
public class SellerDataInitializer implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SellerService sellerService;
    private final CertificationService certificationService;

    public SellerDataInitializer(AuthService authService, UserRepository userRepository,
                                  SellerService sellerService, CertificationService certificationService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.sellerService = sellerService;
        this.certificationService = certificationService;
    }

    private void addCerts(Long userId, String... certs) {
        LocalDate expiry = LocalDate.of(2028, 1, 1);
        for (String cert : certs) {
            certificationService.addCertification(userId, cert, "PRYM Certified", expiry);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Seller Data Initializer running...");

        if (!userRepository.existsByEmail("green_valley@prym.com")) {
            User u = authService.register("green_valley@prym.com", "sellerPass1", User.Role.SELLER,
                    "green_valley", "Green", "Valley", "4165550001", null, "M5V 3A8"); // Toronto
            sellerService.createSellerProfile(u.getId(), "Green Valley Farm", "100 Farm Rd, Toronto", "Locally raised, certified organic beef.");
            addCerts(u.getId(), "ORGANIC", "GRASS_FED");
            sellerService.setRating(u.getId(), 4.8, 42);
        }

        if (!userRepository.existsByEmail("halal_haven@prym.com")) {
            User u = authService.register("halal_haven@prym.com", "sellerPass1", User.Role.SELLER,
                    "halal_haven", "Halal", "Haven", "2125550002", null, "10001"); // New York
            sellerService.createSellerProfile(u.getId(), "Halal Haven Ranch", "200 Ranch Ave, New York", "Premium halal-certified beef and lamb.");
            addCerts(u.getId(), "HALAL", "ORGANIC");
            sellerService.setRating(u.getId(), 4.6, 87);
        }

        if (!userRepository.existsByEmail("prairie_pure@prym.com")) {
            User u = authService.register("prairie_pure@prym.com", "sellerPass1", User.Role.SELLER,
                    "prairie_pure", "Prairie", "Pure", "3125550003", null, "60601"); // Chicago
            sellerService.createSellerProfile(u.getId(), "Prairie Pure Meats", "300 Prairie St, Chicago", "Kosher certified, non-GMO meats.");
            addCerts(u.getId(), "KOSHER", "NON_GMO");
            sellerService.setRating(u.getId(), 4.2, 31);
        }

        if (!userRepository.existsByEmail("pacific_coast@prym.com")) {
            User u = authService.register("pacific_coast@prym.com", "sellerPass1", User.Role.SELLER,
                    "pacific_coast", "Pacific", "Coast", "3105550004", null, "90001"); // Los Angeles
            sellerService.createSellerProfile(u.getId(), "Pacific Coast Butcher", "400 Coast Blvd, Los Angeles", "Animal welfare approved, organic, non-GMO.");
            addCerts(u.getId(), "ORGANIC", "ANIMAL_WELFARE_APPROVED", "NON_GMO");
            sellerService.setRating(u.getId(), 4.9, 156);
        }

        if (!userRepository.existsByEmail("lone_star@prym.com")) {
            User u = authService.register("lone_star@prym.com", "sellerPass1", User.Role.SELLER,
                    "lone_star", "Lone", "Star", "2145550005", null, "75201"); // Dallas
            sellerService.createSellerProfile(u.getId(), "Lone Star Ranch", "500 Star Rd, Dallas", "Halal and grass-fed beef from Texas.");
            addCerts(u.getId(), "HALAL", "GRASS_FED");
            sellerService.setRating(u.getId(), 4.4, 63);
        }

        if (!userRepository.existsByEmail("cascade_farms@prym.com")) {
            User u = authService.register("cascade_farms@prym.com", "sellerPass1", User.Role.SELLER,
                    "cascade_farms", "Cascade", "Farms", "2065550006", null, "98101"); // Seattle
            sellerService.createSellerProfile(u.getId(), "Cascade Farms", "600 Cascade Way, Seattle", "Certified organic, grass-fed, animal welfare approved.");
            addCerts(u.getId(), "ORGANIC", "GRASS_FED", "ANIMAL_WELFARE_APPROVED");
            sellerService.setRating(u.getId(), 4.7, 94);
        }

        if (!userRepository.existsByEmail("liberty_meats@prym.com")) {
            User u = authService.register("liberty_meats@prym.com", "sellerPass1", User.Role.SELLER,
                    "liberty_meats", "Liberty", "Meats", "6175550007", null, "02101"); // Boston
            sellerService.createSellerProfile(u.getId(), "Liberty Meats", "700 Liberty St, Boston", "Kosher, halal, and organic certified.");
            addCerts(u.getId(), "KOSHER", "HALAL", "ORGANIC");
            sellerService.setRating(u.getId(), 4.5, 78);
        }

        if (!userRepository.existsByEmail("rocky_ridge@prym.com")) {
            User u = authService.register("rocky_ridge@prym.com", "sellerPass1", User.Role.SELLER,
                    "rocky_ridge", "Rocky", "Ridge", "7205550008", null, "80201"); // Denver
            sellerService.createSellerProfile(u.getId(), "Rocky Ridge Ranch", "800 Ridge Rd, Denver", "Grass-fed and non-GMO certified beef.");
            addCerts(u.getId(), "GRASS_FED", "NON_GMO");
            sellerService.setRating(u.getId(), 4.3, 45);
        }

        if (!userRepository.existsByEmail("magnolia_meats@prym.com")) {
            User u = authService.register("magnolia_meats@prym.com", "sellerPass1", User.Role.SELLER,
                    "magnolia_meats", "Magnolia", "Meats", "4045550009", null, "30301"); // Atlanta
            sellerService.createSellerProfile(u.getId(), "Magnolia Meats", "900 Magnolia Ln, Atlanta", "Halal, organic, and grass-fed certified.");
            addCerts(u.getId(), "HALAL", "ORGANIC", "GRASS_FED");
            sellerService.setRating(u.getId(), 4.8, 112);
        }

        if (!userRepository.existsByEmail("sunrise_butcher@prym.com")) {
            User u = authService.register("sunrise_butcher@prym.com", "sellerPass1", User.Role.SELLER,
                    "sunrise_butcher", "Sunrise", "Butcher", "2105550010", null, "78201"); // San Antonio
            sellerService.createSellerProfile(u.getId(), "Sunrise Butcher Co.", "1000 Sunrise Blvd, San Antonio", "Kosher and non-GMO meats.");
            addCerts(u.getId(), "KOSHER", "NON_GMO");
            sellerService.setRating(u.getId(), 4.1, 27);
        }

        System.out.println("Seller seed data complete.");
    }
}
