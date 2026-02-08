package com.prym.backend.controller;

import com.prym.backend.model.User; 
import com.prym.backend.repository.UserRepository; 

import org.springframework.http.ResponseEntity; //HTTP response that backend sends back to the frontend
import org.springframework.web.bind.annotation.*; //connects Java code to HTTP request from the website
import java.util.Optional; //a container that may or may not hold any value

@RestController
@RequestMapping("/api/sellers")

public class SellerController {
	
	private UserRepository userRepository;
	
	public SellerController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<User> getSeller(@PathVariable Long id){
		
		Optional<User> user = this.userRepository.findById(id);
		
		if(user.isEmpty()) { //if user does not exist
			return ResponseEntity.notFound().build();
		} else {
			User existingUser = user.get(); //to get the User from Optional<User>
			
			if(existingUser.getRole() == User.Role.BUYER) {
				return ResponseEntity.status(403).body(null);
			} else {
				return ResponseEntity.ok(existingUser);
			}
		}
	}
	
	
}
