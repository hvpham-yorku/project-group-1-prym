package com.prym.backend.service;

import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.util.ZipCodeUtil;

import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User register(String email, String password, User.Role role, String username, String firstName,
            String lastName, String phoneNumber, String profilePicture, String zipCode) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken");
        }

        // Validate and process ZIP/postal code
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            String trimmedZip = zipCode.trim();
            if (!ZipCodeUtil.isValidZip(trimmedZip)) {
                throw new RuntimeException("Invalid ZIP/postal code. Please enter a valid US ZIP code (12345) or Canadian postal code (A1A 1A1).");
            }
        } else {
            throw new RuntimeException("ZIP/postal code is required");
        }

        // Create new user with hashed password
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setProfilePicture(profilePicture);

        // Set location data from ZIP code
        String trimmedZip = zipCode.trim();
        double[] coords = ZipCodeUtil.getCoordinates(trimmedZip);
        user.setZipCode(trimmedZip);
        user.setLatitude(coords[0]);
        user.setLongitude(coords[1]);

        return userRepository.save(user);
    }

    public Optional<User> login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Check if password matches
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public User updateUserInfo(Long userId, String firstName, String lastName, String email, String username,
            String profilePicture, String zipCode) {
        // find the user
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName.trim());
        }
        if (email != null && !email.isBlank()) {
            String trimmed = email.trim();
            if (!trimmed.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmail(trimmed)) {
                    throw new RuntimeException("Email already in use");
                }
                user.setEmail(trimmed);
            }
        }

        if (username != null && !username.isBlank()) {
            String trimmed = username.trim();
            if (!trimmed.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsername(trimmed)) {
                    throw new RuntimeException("Username already taken");
                }
                user.setUsername(trimmed);
            }
        }

        if (profilePicture != null) {
            user.setProfilePicture(profilePicture);
        }

        //Update ZIP/postal code and coordinates if provided and changed
        if (zipCode != null && !zipCode.isBlank()) {
            String trimmedZip = zipCode.trim();
            //Only update if different from current ZIP
            if (user.getZipCode() == null || !trimmedZip.equals(user.getZipCode())) {
                if (!ZipCodeUtil.isValidZip(trimmedZip)) {
                    throw new RuntimeException("Invalid ZIP/postal code. Please enter a valid US ZIP code (12345) or Canadian postal code (A1A 1A1).");
                }
                double[] coords = ZipCodeUtil.getCoordinates(trimmedZip);
                user.setZipCode(trimmedZip);
                user.setLatitude(coords[0]);
                user.setLongitude(coords[1]);
            }
        }

        return userRepository.save(user);
    }

}
