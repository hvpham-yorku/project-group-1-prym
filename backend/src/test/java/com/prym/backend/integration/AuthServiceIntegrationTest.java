package com.prym.backend.integration;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

//Loads the full Spring application context and tests against the real PostgreSQL database.
// @Transactional rolls back all DB changes after each test, so tests don't leave dirty data.
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    // Test 1: register_PersistsUserToDatabase — verifies the user is actually saved and readable
    @Test
    void register_PersistsUserToDatabase() {
        User user = authService.register(
                "integration_test_user@example.com",
                "securePass123",
                User.Role.BUYER,
                "integration_test_username",
                "Integration",
                "Test",
                "416-555-0001",
                null,
                "10001"
        );

        assertNotNull(user.getId()); // ID is assigned by the DB — proves it was actually inserted
        assertEquals("integration_test_user@example.com", user.getEmail());
        assertEquals(User.Role.BUYER, user.getRole());
        assertNotEquals("securePass123", user.getPasswordHash()); // password must be hashed
    }

    // Test 2: register_DuplicateEmail_ThrowsException — verifies the unique constraint on email
    @Test
    void register_DuplicateEmail_ThrowsException() {
        authService.register(
                "dup_email_test@example.com",
                "pass1",
                User.Role.BUYER,
                "dup_email_user1",
                "Dup",
                "Email",
                "416-555-0002",
                null,
                "10001"
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.register(
                        "dup_email_test@example.com", // same email
                        "pass2",
                        User.Role.BUYER,
                        "dup_email_user2",
                        "Dup",
                        "Email2",
                        "416-555-0003",
                        null,
                        "10001"
                )
        );

        assertEquals("Email already registered", ex.getMessage());
    }

    // Test 3: register_DuplicateUsername_ThrowsException — verifies the unique constraint on username
    @Test
    void register_DuplicateUsername_ThrowsException() {
        authService.register(
                "dup_username1@example.com",
                "pass1",
                User.Role.BUYER,
                "dup_username_shared",
                "User",
                "One",
                "416-555-0004",
                null,
                "10001"
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.register(
                        "dup_username2@example.com",
                        "pass2",
                        User.Role.BUYER,
                        "dup_username_shared", // same username
                        "User",
                        "Two",
                        "416-555-0005",
                        null,
                        "10001"
                )
        );

        assertEquals("Username already taken", ex.getMessage());
    }

    // Test 4: login_WithCorrectCredentials_ReturnsUser — verifies the full register→login flow
    @Test
    void login_WithCorrectCredentials_ReturnsUser() {
        authService.register(
                "login_test@example.com",
                "myPassword",
                User.Role.SELLER,
                "login_test_user",
                "Login",
                "Test",
                "416-555-0006",
                null,
                "10001"
        );

        Optional<User> result = authService.login("login_test@example.com", "myPassword");

        assertTrue(result.isPresent());
        assertEquals("login_test@example.com", result.get().getEmail());
        assertEquals(User.Role.SELLER, result.get().getRole());
    }

    // Test 5: login_WithWrongPassword_ReturnsEmpty — wrong password fails against real hashed password
    @Test
    void login_WithWrongPassword_ReturnsEmpty() {
        authService.register(
                "wrong_pass_test@example.com",
                "correctPassword",
                User.Role.BUYER,
                "wrong_pass_user",
                "Wrong",
                "Pass",
                "416-555-0007",
                null,
                "10001"
        );

        Optional<User> result = authService.login("wrong_pass_test@example.com", "wrongPassword");

        assertFalse(result.isPresent());
    }

    // Test 6: updateUserInfo_PersistsChangesToDatabase — verifies updates are saved
    @Test
    void updateUserInfo_PersistsChangesToDatabase() {
        User user = authService.register(
                "update_test@example.com",
                "pass123",
                User.Role.BUYER,
                "update_test_user",
                "Old",
                "Name",
                "416-555-0008",
                null,
                "10001"
        );

        User updated = authService.updateUserInfo(
                user.getId(),
                "New",
                "Name",
                "update_test@example.com",
                "update_test_user",
                null,
                null,
                null
        );

        assertEquals("New", updated.getFirstName());
        assertEquals("Name", updated.getLastName());
    }
}
