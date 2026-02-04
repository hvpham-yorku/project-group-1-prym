package com.prym.backend.repository;

import com.prym.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { //extending JpaRepository gives us free CRUD methods like save,delete,findAll,FindById etc...
   //Optional<User> safely handles the "user not found" errors and avoids NullPointerException 
    Optional<User> findByEmail(String email); //Spring generates and auto SQL query SELECT * FROM users WHERE email = ?
    
    boolean existsByEmail(String email); //returns true if email exists and false otherwise
}