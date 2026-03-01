package com.prym.backend.config;

import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.SellerService;
import com.prym.backend.repository.UserRepository;

@Component
public class SellerDataInitializer implements CommandLineRunner {
	
	private final AuthService authService;
	private final UserRepository userRepository;
	private final SellerService sellerService;
	
	public SellerDataInitializer(AuthService authService, UserRepository userRepository, SellerService sellerService) {
		this.authService = authService;
		this.userRepository = userRepository;
		this.sellerService = sellerService;
	}
	
	@Override
	public void run(String... args) throws Exception{
		System.out.println("Seller Data Initializer running...");
		 
		if(!this.userRepository.existsByEmail("seller1@test.com")) {
			User sellerUser1 = this.authService.register(
					"seller1@test.com", 
					"sellerPass1", 
					User.Role.SELLER,
					"seller1username",
					"Seller",
					"One",
					"1234567890",
					null);
			
			this.sellerService.createSellerProfile(sellerUser1.getId(), "Seller One Shop", "123 Main St");
			System.out.println("Seller 1 created.");
		}
		
		if(!this.userRepository.existsByEmail("seller2@test.com")) {
			User sellerUser2 = this.authService.register(
					"seller2@test.com", 
					"sellerPass2", 
					User.Role.SELLER,
					"seller2username",
					"Seller",
					"Two",
					"1212121212",
					null);
			
		    this.sellerService.createSellerProfile(sellerUser2.getId(), "Seller Two Shop", "456 Market St");
			System.out.println("Seller 2 created.");
		}
	}

}
