package com.prym.backend.config;

import com.prym.backend.repository.BuyerGroupRepository;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.GroupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class GroupDataInitializer implements CommandLineRunner {

    private final GroupService groupService;
    private final UserRepository userRepository;
    private final BuyerGroupRepository buyerGroupRepository;

    public GroupDataInitializer(GroupService groupService, UserRepository userRepository,
                                 BuyerGroupRepository buyerGroupRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
        this.buyerGroupRepository = buyerGroupRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Group Data Initializer running...");

        if (buyerGroupRepository.count() > 0) {
            System.out.println("Groups already exist, skipping.");
            return;
        }

        Long buyer1Id = userRepository.findByEmail("buyer1@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer2Id = userRepository.findByEmail("buyer2@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer3Id = userRepository.findByEmail("buyer3@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer4Id = userRepository.findByEmail("buyer4@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer5Id = userRepository.findByEmail("buyer5@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer6Id = userRepository.findByEmail("buyer6@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer7Id = userRepository.findByEmail("buyer7@prym.com").map(u -> u.getId()).orElse(null);
        Long buyer8Id = userRepository.findByEmail("buyer8@prym.com").map(u -> u.getId()).orElse(null);

        if (buyer1Id == null) {
            System.out.println("Buyers not found, skipping group seed.");
            return;
        }

        groupService.createGroup(buyer1Id, "Organic Circle",          "ORGANIC");
        groupService.createGroup(buyer2Id, "Halal Buyers Group",      "HALAL");
        groupService.createGroup(buyer3Id, "Kosher Crew",             "KOSHER");
        groupService.createGroup(buyer4Id, "Grass Fed Gang",          "GRASS_FED");
        groupService.createGroup(buyer5Id, "Non-GMO Naturals",        "NON_GMO");
        groupService.createGroup(buyer6Id, "Animal Welfare First",    "ANIMAL_WELFARE_APPROVED,ORGANIC");
        groupService.createGroup(buyer7Id, "Premium All-Certs",       "KOSHER,HALAL,ORGANIC,GRASS_FED,NON_GMO");
        groupService.createGroup(buyer8Id, "No Preference Group",     "");

        System.out.println("Group seed data complete.");
    }
}
