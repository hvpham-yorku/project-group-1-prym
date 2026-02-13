package com.prym.backend.dto;

import org.springframework.web.bind.annotation.RequestMapping;

//what lombok does is it basically auto generates getters, setters,constructors, and many basic functions like toString.
//using lombok is a design choice we decided on just to make the code more readable and less repetitive
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data//lombok generates all the getters, setters, and toString function without needing to type them out!
@NoArgsConstructor //this is the beauty of lombok, by using this tag it generates the default constructor without having to type it
@AllArgsConstructor//this tag generates the constructor that takes in all arguments, reduced about 10 lines of code into 1!

@RequestMapping("/api/sellers")
public class SellerProfileUpdateDTO {
	
	private String userName;
	private String firstName;
	private String lastName;
	private String email;
	private String phoneNumber;
	private String profilePicture;
	//id, password, createdAt are not allowed to be updated here

}
