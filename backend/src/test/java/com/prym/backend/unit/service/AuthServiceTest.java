package com.prym.backend.unit.service;
import com.prym.backend.service.AuthService;

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

   
    private AuthService authService;

    
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
                "newuser", "Jane", "Doe", "416-555-1111", null, "10001");

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
                        "newuser", "A", "B", "416-000-0000", null, "10001"));

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
                        "testuser", "A", "B", "416-000-0000", null, "10001"));

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

    // Test 7: updateUserInfo_Success
    @Test
    public void updateUserInfo_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = authService.updateUserInfo(1L, "Jane", "Doe", "new@example.com", "newusername", null, null, null);

        assertEquals("Jane", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("newusername", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    // Test 8: updateUserInfo_UserNotFound
    @Test
    public void updateUserInfo_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateUserInfo(99L, "Jane", "Doe", "new@example.com", "newusername", null, null, null));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 9: updateUserInfo_EmailAlreadyInUse
    @Test
    public void updateUserInfo_EmailAlreadyInUse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateUserInfo(1L, "Test", "User", "taken@example.com", "testuser", null, null, null));

        assertEquals("Email already in use", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 10: updateUserInfo_UsernameAlreadyTaken
    @Test
    public void updateUserInfo_UsernameAlreadyTaken() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateUserInfo(1L, "Test", "User", "test@example.com", "takenuser", null, null, null));

        assertEquals("Username already taken", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // Test 11: updateUserInfo_IgnoresBlankFields
    @Test
    public void updateUserInfo_IgnoresBlankFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        User result = authService.updateUserInfo(1L, "  ", "  ", "test@example.com", "testuser", null, null, null);

        assertEquals("Test", result.getFirstName());
        assertEquals("User", result.getLastName());
        verify(userRepository).save(any(User.class));
    }
}
