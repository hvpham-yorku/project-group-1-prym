package com.prym.backend.controller;

import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads ONLY the AuthController (not the full app), making the test fast.
// @AutoConfigureMockMvc(addFilters = false) skips Spring Security filters so we can
// test the controller logic without needing authentication setup.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginTest {

    // MockMvc lets us send fake HTTP requests to our controller without starting a real server
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean creates a fake version of AuthService so we can control what it returns
    @MockitoBean
    private AuthService authService;

    // ---- Test 1: Successful login with correct credentials ----
    @Test
    void loginWithValidCredentials_returnsUserData() throws Exception {
        // Arrange: create a fake user that the mock service will return
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setRole(User.Role.BUYER);

        // Tell the mock: when login() is called with these exact credentials, return the user
        when(authService.login("test@example.com", "password123"))
                .thenReturn(Optional.of(mockUser));

        // Act & Assert: send a POST to /api/auth/login and verify the response
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isOk())                          // expect 200 OK
                .andExpect(jsonPath("$.email").value("test@example.com")) // check email in JSON
                .andExpect(jsonPath("$.role").value("BUYER"));           // check role in JSON
    }

    // ---- Test 2: Failed login with wrong credentials ----
    @Test
    void loginWithInvalidCredentials_returns401() throws Exception {
        // Arrange: the mock returns empty (meaning no user found / wrong password)
        when(authService.login(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert: send a login request and expect a 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"wrong@example.com\", \"password\": \"wrongpass\"}"))
                .andExpect(status().isUnauthorized())                        // expect 401
                .andExpect(content().string("Invalid email or password"));   // check error message
    }

    // ---- Test 3: Login with empty/missing fields still responds gracefully ----
    @Test
    void loginWithEmptyBody_returns401() throws Exception {
        // Arrange: when email and password are null (missing from JSON), service returns empty
        when(authService.login(null, null))
                .thenReturn(Optional.empty());

        // Act & Assert: send an empty JSON body and expect 401, not a server crash
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
