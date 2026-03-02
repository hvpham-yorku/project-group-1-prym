package com.prym.backend.service;

import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash(encoder.encode("password123"));
        testUser.setRole(User.Role.BUYER);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
    }

    // Test 1: Successful registration saves user and returns it
    @Test
    void register_Success() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = authService.register("new@example.com", "password123", User.Role.BUYER,
                "newuser", "Jane", "Smith", "555-0000", null);

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("newuser", result.getUsername());
        assertEquals(User.Role.BUYER, result.getRole());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        verify(userRepository).save(any(User.class));
    }

    // Test 2: Register fails when email is already taken
    @Test
    void register_EmailAlreadyTaken() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.register("test@example.com", "password123", User.Role.BUYER,
                        "anotheruser", "Jane", "Smith", "555-0000", null));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 3: Register fails when username is already taken
    @Test
    void register_UsernameAlreadyTaken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.register("new@example.com", "password123", User.Role.BUYER,
                        "testuser", "Jane", "Smith", "555-0000", null));

        assertEquals("Username already taken", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 4: Successful login returns the user
    @Test
    void login_ValidCredentials() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = authService.login("test@example.com", "password123");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    // Test 5: Login fails when password is wrong
    @Test
    void login_WrongPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = authService.login("test@example.com", "wrongpassword");

        assertTrue(result.isEmpty());
    }

    // Test 6: Login fails when email is not registered
    @Test
    void login_EmailNotFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Optional<User> result = authService.login("nobody@example.com", "password123");

        assertTrue(result.isEmpty());
    }

    // Test 7: Password is stored as a BCrypt hash, not plain text
    @Test
    void register_PasswordIsHashed() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = authService.register("new@example.com", "plaintext", User.Role.BUYER,
                "user", "A", "B", "555", null);

        assertNotEquals("plaintext", result.getPasswordHash());
        assertTrue(result.getPasswordHash().startsWith("$2a$") || result.getPasswordHash().startsWith("$2b$"));
    }
}
