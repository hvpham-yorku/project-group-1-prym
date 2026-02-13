package com.prym.backend.model;

import jakarta.persistence.*;
//what lombok does is it basically auto generates getters, setters,constructors, and many basic functions like toString.
//using lombok is a design choice we decided on just to make the code more readable and less repetitive
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data//lombok generates all the getters, setters, and toString function without needing to type them out!
@NoArgsConstructor //this is the beauty of lombok, by using this tag it generates the default constructor without having to type it
@AllArgsConstructor//this tag generates the constructor that takes in all arguments, reduced about 10 lines of code into 1!
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) //email must be unique and non-empty
    private String email;

    @Column(nullable = false) //password is unique and stored as a hash not string
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phoneNumber;

    @Lob  // Large Object tag for storing image data 
    @Column(columnDefinition = "LONGTEXT")
    private String profilePicture;  // Store as Base64 string

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Role { //enum is safer to use than strings
        BUYER, SELLER
    }
}