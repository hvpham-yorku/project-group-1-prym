package com.prym.backend.controller;

import com.prym.backend.model.Session; // Imports Session model so we can create fake session objects
import com.prym.backend.model.User; // Imports User model for creation of fake User objects
import com.prym.backend.service.AuthService; // Imports AuthService for creation of mock version of it
import com.prym.backend.service.SellerService; // Imports SellerService — controller requires it, so we must mock it
import com.prym.backend.service.SessionService; // Imports SessionService — controller creates sessions on login
import org.junit.jupiter.api.Test; // So that we can use @Test to make JUnit tests run
import org.springframework.beans.factory.annotation.Autowired; // Tells Spring to automatically inject the MockMvc instance
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // Configures MockMvc; addFilters=false disables Spring Security
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest; // Tells Spring to only load the web layer (just controller not whole app)
import org.springframework.http.MediaType; // Used to tell the request that we're sending JSON data
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Creates a fake version of a class and puts it in Spring's context
import org.springframework.test.web.servlet.MockMvc; // Tool that simulates HTTP requests without a real running server

import java.util.Optional; // Java wrapper that can hold a value or be empty (used by AuthService.login)

import static org.mockito.ArgumentMatchers.any; // Mockito matcher meaning "any object of this type"
import static org.mockito.ArgumentMatchers.anyString; // Mockito matcher meaning "any string value at all"
import static org.mockito.Mockito.when; // Used to tell the mock "when this method is called, do this"
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // Builds a fake POST request to send via MockMvc
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // Imports status(), content(), jsonPath() for checking HTTP responses

// @WebMvcTest loads ONLY the AuthController (not the full app)
@WebMvcTest(AuthController.class)
// Skips Spring Security filters so we can test controller logic without needing authentication setup
@AutoConfigureMockMvc(addFilters = false)
class LoginTest {

    // Spring automatically provides a MockMvc instance here (our fake HTTP client)
    @Autowired
    private MockMvc mockMvc;

    // Creates a fake AuthService so we can control what login() returns
    @MockitoBean
    private AuthService authService;

    // Creates a fake SessionService — the controller calls this on login to create a session cookie
    // We must mock it even though we don't test sessions directly, because the controller needs it to exist
    @MockitoBean
    private SessionService sessionService;

    // Creates a fake SellerService — the controller has this as a dependency
    // We must mock it so Spring can create the controller, even though login doesn't use it
    @MockitoBean
    private SellerService sellerService;

    // ---- Test 1: Successful login with correct credentials ----
    @Test
    void loginWithValidCredentials_returnsUserData() throws Exception {
        // Create a fake user with all the fields the controller's buildUserResponse() returns
        User mockUser = new User();
        mockUser.setId(1L); // Fake ID (1 of type long, matching the Long field in User.java)
        mockUser.setEmail("test@example.com"); // The email we'll log in with
        mockUser.setRole(User.Role.BUYER); // Set role to BUYER
        mockUser.setUsername("testuser"); // New field — the user's display name
        mockUser.setFirstName("Test"); // New field — first name
        mockUser.setLastName("User"); // New field — last name
        mockUser.setPhoneNumber("555-1234"); // New field — phone number
        mockUser.setProfilePicture(null); // New field — no profile picture for this test

        // Create a fake session that SessionService will return when called
        Session mockSession = new Session();
        mockSession.setSessionId("fake-session-id"); // The session ID that would go into a cookie

        // Tell the mock: when login() is called with these exact credentials, return the user
        when(authService.login("test@example.com", "password123"))
                .thenReturn(Optional.of(mockUser));

        // Tell the mock: when createSession() is called with any User, return our fake session
        // any(User.class) means "any User object" — we don't care which specific user, just return the session
        when(sessionService.createSession(any(User.class)))
                .thenReturn(mockSession);

        // Send a fake POST request to /api/auth/login and verify the response
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON) // Tell the server we're sending JSON
                        .content("{\"email\": \"test@example.com\", \"password\": \"password123\"}")) // The JSON body
                .andExpect(status().isOk()) // Expect HTTP status 200 OK
                .andExpect(jsonPath("$.email").value("test@example.com")) // Check email in the JSON response
                .andExpect(jsonPath("$.role").value("BUYER")) // Check role is BUYER
                .andExpect(jsonPath("$.username").value("testuser")) // Check username is returned
                .andExpect(jsonPath("$.firstName").value("Test")) // Check firstName is returned
                .andExpect(jsonPath("$.lastName").value("User")); // Check lastName is returned
    }

    // ---- Test 2: Failed login with wrong credentials ----
    @Test
    void loginWithInvalidCredentials_returns401() throws Exception {
        // anyString() means "no matter what email or password is passed in"
        // Return Optional.empty() meaning "no user found / wrong password"
        when(authService.login(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Send a login request with wrong credentials
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"wrong@example.com\", \"password\": \"wrongpass\"}"))
                .andExpect(status().isUnauthorized()) // Expect HTTP 401 Unauthorized
                // The error is now returned as JSON: {"error": "Invalid email or password"}
                // jsonPath("$.error") reads the "error" field from the JSON response
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // ---- Test 3: Login with empty/missing fields ----
    @Test
    void loginWithEmptyBody_returns401() throws Exception {
        // When email and password are null (because {} has no fields), service returns empty
        when(authService.login(null, null))
                .thenReturn(Optional.empty());

        // Send an empty JSON body and expect 401, not a 500 server crash
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty JSON — both email and password will be null in the controller
                .andExpect(status().isUnauthorized()); // Expect 401, the app handles missing fields gracefully
    }
}
