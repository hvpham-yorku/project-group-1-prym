// Declares which package this test class belongs to (matches the folder structure)
package com.prym.backend.controller;

// Imports the Session model so we can create fake session objects for cookie creation
import com.prym.backend.model.Session;
// Imports the User model so we can create fake User objects in our tests
import com.prym.backend.model.User;
// Imports AuthService so we can create a mock (fake) version of it
import com.prym.backend.service.AuthService;
// Imports SellerService — the seller registration endpoint calls this to create a seller profile
import com.prym.backend.service.SellerService;
// Imports SessionService — both endpoints create sessions on successful registration
import com.prym.backend.service.SessionService;
// Imports @Test — this annotation marks a method as a test that JUnit will run
import org.junit.jupiter.api.Test;
// Imports @Autowired — tells Spring to automatically inject (provide) the MockMvc instance for us
import org.springframework.beans.factory.annotation.Autowired;
// Imports @AutoConfigureMockMvc — configures MockMvc; addFilters=false disables Spring Security
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
// Imports @WebMvcTest — tells Spring to only load the web layer (just the controller, not the whole app)
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// Imports MediaType — used to tell the request that we're sending JSON data
import org.springframework.http.MediaType;
// Imports @MockitoBean — creates a fake (mock) version of a class and puts it in Spring's context
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// Imports MockMvc — the tool that simulates HTTP requests without a real running server
import org.springframework.test.web.servlet.MockMvc;

// Imports any() — a Mockito matcher meaning "any object of this type"
import static org.mockito.ArgumentMatchers.any;
// Imports eq() — a Mockito matcher meaning "exactly equal to this value"
import static org.mockito.ArgumentMatchers.eq;
// Imports when() — used to tell the mock "when this method is called, do this"
import static org.mockito.Mockito.when;
// Imports post() — builds a fake POST request to send via MockMvc
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Imports status(), content(), jsonPath() — used to check the HTTP response
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Only loads AuthController (not the full app with database, etc.) — makes the test fast
@WebMvcTest(AuthController.class)
// Disables Spring Security filters so we don't need auth tokens just to test the controller
@AutoConfigureMockMvc(addFilters = false)
// The test class itself — no "public" keyword needed in modern Java
class RegisterTest {

    // Spring automatically provides a MockMvc instance here (our fake HTTP client)
    @Autowired
    private MockMvc mockMvc;

    // Creates a fake AuthService — we control what it returns instead of hitting a real database
    @MockitoBean
    private AuthService authService;

    // Creates a fake SessionService — the controller creates a session cookie on registration
    @MockitoBean
    private SessionService sessionService;

    // Creates a fake SellerService — the seller registration endpoint calls this to create a seller profile
    @MockitoBean
    private SellerService sellerService;

    // ---- Test 1: Successfully register a buyer account ----

    // Marks this method as a test case that JUnit should run
    @Test
    // The method name describes what we're testing — "throws Exception" is required by MockMvc
    void registerBuyer_returnsUserData() throws Exception {

        // Create a fake user with all the fields the frontend sends during registration
        User mockUser = new User();
        mockUser.setId(1L); // Fake ID (1 of type long, matching the Long field in User.java)
        mockUser.setEmail("buyer@example.com"); // The email used for registration
        mockUser.setRole(User.Role.BUYER); // Role is BUYER since we're hitting the buyer endpoint
        mockUser.setUsername("buyeruser"); // The chosen username
        mockUser.setFirstName("Jane"); // First name
        mockUser.setLastName("Doe"); // Last name
        mockUser.setPhoneNumber("555-1234"); // Phone number
        mockUser.setProfilePicture(null); // No profile picture in this test

        // Create a fake session — the controller creates a session cookie after registration
        Session mockSession = new Session();
        mockSession.setSessionId("fake-session-id"); // The session ID that would go into the cookie

        // Tell the mock: when register() is called with all these exact values, return our fake user
        // register() now takes 8 parameters: email, password, role, username, firstName, lastName, phoneNumber, profilePicture
        when(authService.register("buyer@example.com", "password123", User.Role.BUYER,
                "buyeruser", "Jane", "Doe", "555-1234", null))
                .thenReturn(mockUser);

        // Tell the mock: when createSession() is called with any User, return our fake session
        when(sessionService.createSession(any(User.class)))
                .thenReturn(mockSession);

        // Use MockMvc to send a fake POST request to /api/auth/register/buyer
        mockMvc.perform(post("/api/auth/register/buyer")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // The JSON body — includes all the new fields the frontend now sends
                        .content("{\"email\": \"buyer@example.com\", \"password\": \"password123\", "
                                + "\"username\": \"buyeruser\", \"firstName\": \"Jane\", "
                                + "\"lastName\": \"Doe\", \"phoneNumber\": \"555-1234\"}"))
                // Check that the HTTP status code is 200 (OK)
                .andExpect(status().isOk())
                // Check that the JSON response has the correct email
                .andExpect(jsonPath("$.email").value("buyer@example.com"))
                // Check that the role is BUYER
                .andExpect(jsonPath("$.role").value("BUYER"))
                // Check the new fields are returned in the response
                .andExpect(jsonPath("$.username").value("buyeruser"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    // ---- Test 2: Successfully register a seller account ----

    @Test
    void registerSeller_returnsUserData() throws Exception {

        // Create a fake seller user
        User mockUser = new User();
        mockUser.setId(2L); // Different ID than the buyer test
        mockUser.setEmail("seller@example.com");
        mockUser.setRole(User.Role.SELLER); // Set the role to SELLER this time
        mockUser.setUsername("selleruser");
        mockUser.setFirstName("John");
        mockUser.setLastName("Smith");
        mockUser.setPhoneNumber("555-5678");
        mockUser.setProfilePicture(null);

        // Fake session for the cookie
        Session mockSession = new Session();
        mockSession.setSessionId("fake-session-id");

        // Tell the mock: when register() is called with SELLER role, return our fake seller
        when(authService.register("seller@example.com", "password123", User.Role.SELLER,
                "selleruser", "John", "Smith", "555-5678", null))
                .thenReturn(mockUser);

        // Tell the mock: when createSession() is called, return our fake session
        when(sessionService.createSession(any(User.class)))
                .thenReturn(mockSession);

        // Send a POST to the seller registration endpoint
        mockMvc.perform(post("/api/auth/register/seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"seller@example.com\", \"password\": \"password123\", "
                                + "\"username\": \"selleruser\", \"firstName\": \"John\", "
                                + "\"lastName\": \"Smith\", \"phoneNumber\": \"555-5678\"}"))
                // Expect 200 OK
                .andExpect(status().isOk())
                // Verify the response contains the correct fields
                .andExpect(jsonPath("$.email").value("seller@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.username").value("selleruser"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    // ---- Test 3: Registration fails when email is already taken ----

    @Test
    void registerWithExistingEmail_returns400() throws Exception {

        // Tell the mock: when register() is called with this email, throw an exception
        // This simulates the AuthService detecting a duplicate email in the database
        when(authService.register(eq("taken@example.com"), eq("password123"), eq(User.Role.BUYER),
                any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Email already registered"));

        // Send a POST trying to register with an email that's already in use
        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"taken@example.com\", \"password\": \"password123\", "
                                + "\"username\": \"someone\", \"firstName\": \"A\", "
                                + "\"lastName\": \"B\", \"phoneNumber\": \"555-0000\"}"))
                // Expect 400 Bad Request (not 200 OK)
                .andExpect(status().isBadRequest())
                // The error is now returned as JSON: {"error": "Email already registered"}
                // jsonPath("$.error") reads the "error" field from the JSON response
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }
}
