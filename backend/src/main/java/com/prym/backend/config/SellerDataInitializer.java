package com.prym.backend.config;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.CertificationService;
import com.prym.backend.service.CowTypeService;
import com.prym.backend.service.SellerService;
import com.prym.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

//Seeds the database with test seller/farm accounts on startup.
//Runs at Order(1) so sellers exist before buyers get created.
//Each seller gets a profile, shop info, and some certifications.
@Component
@Order(1)
public class SellerDataInitializer implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SellerService sellerService;
    private final CertificationService certificationService;
    private final CowTypeService cowTypeService;

    public SellerDataInitializer(AuthService authService, UserRepository userRepository,
                                  SellerService sellerService, CertificationService certificationService,
                                  CowTypeService cowTypeService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.sellerService = sellerService;
        this.certificationService = certificationService;
        this.cowTypeService = cowTypeService;
    }

    private void addCowType(Long userId, String breed, String description, double pricePerPound, int availableCount) {
        cowTypeService.createCowType(userId, breed, description, pricePerPound, availableCount);
    }

    //helper to bulk-add certifications with a dummy expiry date
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
            addCowType(u.getId(), "GRASS_FED", "Pasture-raised on open fields, no hormones or antibiotics.", 7.50, 12);
            addCowType(u.getId(), "ANGUS", "Classic Black Angus, organically raised with rich marbling.", 8.25, 8);
            addCowType(u.getId(), "HERITAGE", "Rare heritage breed kept on organic pasture since 1987.", 11.00, 4);
            addCowType(u.getId(), "WAGYU", "Limited-run organic Wagyu — only 3 available per season.", 21.00, 3);
            addCowType(u.getId(), "CONVENTIONAL", "Everyday organic beef at an accessible price point.", 6.50, 18);
        }

        if (!userRepository.existsByEmail("halal_haven@prym.com")) {
            User u = authService.register("halal_haven@prym.com", "sellerPass1", User.Role.SELLER,
                    "halal_haven", "Halal", "Haven", "2125550002", null, "10001"); // New York
            sellerService.createSellerProfile(u.getId(), "Halal Haven Ranch", "200 Ranch Ave, New York", "Premium halal-certified beef and lamb.");
            addCerts(u.getId(), "HALAL", "ORGANIC");
            sellerService.setRating(u.getId(), 4.6, 87);
            addCowType(u.getId(), "WAGYU", "Halal-certified Wagyu, exceptionally tender with intense marbling.", 18.00, 5);
            addCowType(u.getId(), "ANGUS", "Halal-certified Angus, organically raised and hand-selected.", 9.50, 10);
            addCowType(u.getId(), "GRASS_FED", "Halal grass-fed Hereford cross, raised on open New York pastures.", 8.00, 9);
            addCowType(u.getId(), "HERITAGE", "Halal heritage breed, slow-finished on organic grain and hay.", 12.00, 6);
            addCowType(u.getId(), "CONVENTIONAL", "Halal conventional beef, consistent quality at a great price.", 6.75, 14);
        }

        if (!userRepository.existsByEmail("prairie_pure@prym.com")) {
            User u = authService.register("prairie_pure@prym.com", "sellerPass1", User.Role.SELLER,
                    "prairie_pure", "Prairie", "Pure", "3125550003", null, "60601"); // Chicago
            sellerService.createSellerProfile(u.getId(), "Prairie Pure Meats", "300 Prairie St, Chicago", "Kosher certified, non-GMO meats.");
            addCerts(u.getId(), "KOSHER", "NON_GMO");
            sellerService.setRating(u.getId(), 4.2, 31);
            addCowType(u.getId(), "HERITAGE", "Kosher-certified heritage breed, non-GMO feed, slow-grown for flavor.", 10.00, 7);
            addCowType(u.getId(), "ANGUS", "Kosher Angus, non-GMO certified with traditional processing standards.", 8.75, 9);
            addCowType(u.getId(), "GRASS_FED", "Kosher grass-fed shorthorn, non-GMO and raised on Midwest prairie.", 8.25, 11);
            addCowType(u.getId(), "WAGYU", "Kosher Wagyu cross, non-GMO with superior tenderness.", 16.50, 3);
            addCowType(u.getId(), "CONVENTIONAL", "Kosher conventional beef, non-GMO and budget friendly.", 6.00, 20);
        }

        if (!userRepository.existsByEmail("pacific_coast@prym.com")) {
            User u = authService.register("pacific_coast@prym.com", "sellerPass1", User.Role.SELLER,
                    "pacific_coast", "Pacific", "Coast", "3105550004", null, "90001"); // Los Angeles
            sellerService.createSellerProfile(u.getId(), "Pacific Coast Butcher", "400 Coast Blvd, Los Angeles", "Animal welfare approved, organic, non-GMO.");
            addCerts(u.getId(), "ORGANIC", "ANIMAL_WELFARE_APPROVED", "NON_GMO");
            sellerService.setRating(u.getId(), 4.9, 156);
            addCowType(u.getId(), "WAGYU", "Certified organic Wagyu, raised with full animal welfare approval.", 20.00, 4);
            addCowType(u.getId(), "HERITAGE", "Non-GMO heritage breed, free-range with exceptional welfare standards.", 11.50, 6);
            addCowType(u.getId(), "ANGUS", "Organic Black Angus, animal welfare approved and non-GMO verified.", 9.75, 10);
            addCowType(u.getId(), "GRASS_FED", "Organic 100% grass-fed, raised on California coastal pasture.", 8.50, 13);
            addCowType(u.getId(), "CONVENTIONAL", "Organic conventional — certified clean, affordable, and welfare approved.", 7.00, 17);
        }

        if (!userRepository.existsByEmail("lone_star@prym.com")) {
            User u = authService.register("lone_star@prym.com", "sellerPass1", User.Role.SELLER,
                    "lone_star", "Lone", "Star", "2145550005", null, "75201"); // Dallas
            sellerService.createSellerProfile(u.getId(), "Lone Star Ranch", "500 Star Rd, Dallas", "Halal and grass-fed beef from Texas.");
            addCerts(u.getId(), "HALAL", "GRASS_FED");
            sellerService.setRating(u.getId(), 4.4, 63);
            addCowType(u.getId(), "ANGUS", "Halal-certified Texas Angus, 100% grass-fed on sprawling ranch land.", 9.00, 14);
            addCowType(u.getId(), "GRASS_FED", "Halal grass-fed longhorn cross, raised on native Texas pasture.", 7.75, 11);
            addCowType(u.getId(), "WAGYU", "Halal Texas Wagyu — rare, richly marbled, finished on native grass.", 19.50, 3);
            addCowType(u.getId(), "HERITAGE", "Halal heritage longhorn, deeply flavorful and slow-raised.", 10.50, 5);
            addCowType(u.getId(), "CONVENTIONAL", "Halal conventional Texas beef, great value and consistent quality.", 6.25, 22);
        }

        if (!userRepository.existsByEmail("cascade_farms@prym.com")) {
            User u = authService.register("cascade_farms@prym.com", "sellerPass1", User.Role.SELLER,
                    "cascade_farms", "Cascade", "Farms", "2065550006", null, "98101"); // Seattle
            sellerService.createSellerProfile(u.getId(), "Cascade Farms", "600 Cascade Way, Seattle", "Certified organic, grass-fed, animal welfare approved.");
            addCerts(u.getId(), "ORGANIC", "GRASS_FED", "ANIMAL_WELFARE_APPROVED");
            sellerService.setRating(u.getId(), 4.7, 94);
            addCowType(u.getId(), "GRASS_FED", "Organic grass-fed, rotationally grazed on Pacific Northwest pastures.", 8.00, 13);
            addCowType(u.getId(), "HERITAGE", "Animal welfare approved heritage breed, organic and free-range.", 11.00, 6);
            addCowType(u.getId(), "ANGUS", "Organic Pacific Angus, welfare approved and finished on mountain grass.", 9.50, 10);
            addCowType(u.getId(), "WAGYU", "Organic Wagyu, welfare certified and raised in the Cascade foothills.", 20.50, 3);
            addCowType(u.getId(), "CONVENTIONAL", "Organic conventional beef, welfare approved and non-GMO.", 6.75, 19);
        }

        if (!userRepository.existsByEmail("liberty_meats@prym.com")) {
            User u = authService.register("liberty_meats@prym.com", "sellerPass1", User.Role.SELLER,
                    "liberty_meats", "Liberty", "Meats", "6175550007", null, "02101"); // Boston
            sellerService.createSellerProfile(u.getId(), "Liberty Meats", "700 Liberty St, Boston", "Kosher, halal, and organic certified.");
            addCerts(u.getId(), "KOSHER", "HALAL", "ORGANIC");
            sellerService.setRating(u.getId(), 4.5, 78);
            addCowType(u.getId(), "WAGYU", "Dual-certified Kosher and Halal Wagyu, premium organic finish.", 19.00, 4);
            addCowType(u.getId(), "ANGUS", "Certified Kosher and Halal Angus, organically raised in New England.", 9.25, 8);
            addCowType(u.getId(), "GRASS_FED", "Kosher and Halal grass-fed Hereford, organically raised in Vermont.", 8.25, 10);
            addCowType(u.getId(), "HERITAGE", "Triple-certified heritage breed — Kosher, Halal, and organic.", 13.00, 4);
            addCowType(u.getId(), "CONVENTIONAL", "Kosher and Halal conventional, organic-grade feed and handling.", 7.00, 16);
        }

        if (!userRepository.existsByEmail("rocky_ridge@prym.com")) {
            User u = authService.register("rocky_ridge@prym.com", "sellerPass1", User.Role.SELLER,
                    "rocky_ridge", "Rocky", "Ridge", "7205550008", null, "80201"); // Denver
            sellerService.createSellerProfile(u.getId(), "Rocky Ridge Ranch", "800 Ridge Rd, Denver", "Grass-fed and non-GMO certified beef.");
            addCerts(u.getId(), "GRASS_FED", "NON_GMO");
            sellerService.setRating(u.getId(), 4.3, 45);
            addCowType(u.getId(), "GRASS_FED", "High-altitude grass-fed, non-GMO certified Rocky Mountain beef.", 7.50, 16);
            addCowType(u.getId(), "CONVENTIONAL", "Non-GMO conventional beef, affordable and ethically raised.", 6.00, 20);
            addCowType(u.getId(), "ANGUS", "Non-GMO Angus raised at 5,000ft elevation on Rocky Mountain pasture.", 8.75, 12);
            addCowType(u.getId(), "HERITAGE", "Non-GMO heritage Hereford, slow-raised on high-country hay and grass.", 10.25, 7);
            addCowType(u.getId(), "WAGYU", "Non-GMO Rocky Mountain Wagyu, cold-climate raised for dense marbling.", 17.00, 3);
        }

        if (!userRepository.existsByEmail("magnolia_meats@prym.com")) {
            User u = authService.register("magnolia_meats@prym.com", "sellerPass1", User.Role.SELLER,
                    "magnolia_meats", "Magnolia", "Meats", "4045550009", null, "30301"); // Atlanta
            sellerService.createSellerProfile(u.getId(), "Magnolia Meats", "900 Magnolia Ln, Atlanta", "Halal, organic, and grass-fed certified.");
            addCerts(u.getId(), "HALAL", "ORGANIC", "GRASS_FED");
            sellerService.setRating(u.getId(), 4.8, 112);
            addCowType(u.getId(), "WAGYU", "Halal-certified organic Wagyu, pasture-raised in Southern Georgia.", 17.50, 5);
            addCowType(u.getId(), "GRASS_FED", "Halal, organic, grass-fed Angus cross — Southern raised and finished.", 8.50, 10);
            addCowType(u.getId(), "ANGUS", "Halal organic Black Angus, slow-raised on Georgia's red clay pastures.", 9.25, 11);
            addCowType(u.getId(), "HERITAGE", "Halal organic heritage breed, raised on native Southern grasses.", 12.50, 5);
            addCowType(u.getId(), "CONVENTIONAL", "Halal organic conventional — clean, affordable, Southern-raised.", 6.75, 18);
        }

        if (!userRepository.existsByEmail("sunrise_butcher@prym.com")) {
            User u = authService.register("sunrise_butcher@prym.com", "sellerPass1", User.Role.SELLER,
                    "sunrise_butcher", "Sunrise", "Butcher", "2105550010", null, "78201"); // San Antonio
            sellerService.createSellerProfile(u.getId(), "Sunrise Butcher Co.", "1000 Sunrise Blvd, San Antonio", "Kosher and non-GMO meats.");
            addCerts(u.getId(), "KOSHER", "NON_GMO");
            sellerService.setRating(u.getId(), 4.1, 27);
            addCowType(u.getId(), "HERITAGE", "Kosher-certified heritage breed, non-GMO and slow-finished.", 10.50, 7);
            addCowType(u.getId(), "CONVENTIONAL", "Kosher conventional beef, non-GMO with consistent quality.", 6.25, 15);
            addCowType(u.getId(), "ANGUS", "Kosher Angus, non-GMO raised on South Texas coastal plains.", 8.50, 10);
            addCowType(u.getId(), "GRASS_FED", "Kosher grass-fed shorthorn cross, non-GMO and Texas pasture-raised.", 7.75, 9);
            addCowType(u.getId(), "WAGYU", "Kosher non-GMO Wagyu, rare and richly marbled from South Texas.", 16.00, 3);
        }

        System.out.println("Seller seed data complete.");
    }
}
