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

    @Mock
    private AuthService authService;

    @Mock
    private User testUser;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhoneNumber("416-555-0000");
        testUser.setRole(User.Role.BUYER);
        testUser.setPasswordHash(encoder.encode("password123"));
    }

    // Test 1: register_Success
    @Test
    public void register_Success() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = authService.register(
                "new@example.com", "password123", User.Role.BUYER,
                "newuser", "Jane", "Doe", "416-555-1111", null);

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals(User.Role.BUYER, result.getRole());
        assertNotEquals("password123", result.getPasswordHash()); // must NOT be plain text
        assertTrue(encoder.matches("password123", result.getPasswordHash())); // but must match
        verify(userRepository).save(any(User.class));
    }

    // Test 2: register_DuplicateEmail
    @Test
    public void register_DuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("test@example.com", "pass", User.Role.BUYER,
                        "newuser", "A", "B", "416-000-0000", null));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 3: register_DuplicateUsername
    @Test
    public void register_DuplicateUsername() {
        when(userRepository.existsByEmail("other@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("other@example.com", "pass", User.Role.BUYER,
                        "testuser", "A", "B", "416-000-0000", null));

        assertEquals("Username already taken", ex.getMessage());
        verify(userRepository, never()).save(any()); // must NOT save
    }

    // Test 4: login_Success
    @Test
    public void login_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = authService.login("test@example.com", "password123");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    // Test 5: login_WrongPassword
    @Test
    public void login_WrongPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = authService.login("test@example.com", "wrongpassword");

        assertFalse(result.isPresent());
    }

    // Test 6: login_EmailNotFound
    @Test
    public void login_EmailNotFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Optional<User> result = authService.login("nobody@example.com", "password123");

        assertFalse(result.isPresent());
    }
}
