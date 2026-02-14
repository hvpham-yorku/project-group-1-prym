package com.prym.backend.controller;

import com.prym.backend.model.Session; // fake session objects for cookie creation
import com.prym.backend.model.User; // fake User objects
import com.prym.backend.service.AuthService; // mock of auth logic
import com.prym.backend.service.SellerService; // needed by controller for seller registration
import com.prym.backend.service.SessionService; // needed by controller for session cookies
import org.junit.jupiter.api.Test; // marks methods as test cases
import org.springframework.beans.factory.annotation.Autowired; // auto-injects MockMvc
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // configures MockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest; // loads only the controller layer
import org.springframework.http.MediaType; // specifies JSON content type
import org.springframework.test.context.bean.override.mockito.MockitoBean; // creates mock beans
import org.springframework.test.web.servlet.MockMvc; // simulates HTTP requests

import static org.mockito.ArgumentMatchers.any; // matches any object
import static org.mockito.ArgumentMatchers.eq; // matches exact value
import static org.mockito.Mockito.when; // defines mock behavior
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // builds POST requests
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // checks responses

@WebMvcTest(AuthController.class) // only loads AuthController, not the full app
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security for testing
class RegisterTest {

    @Autowired
    private MockMvc mockMvc; // fake HTTP client

    @MockitoBean
    private AuthService authService; // fake auth service (no real database)
    @MockitoBean
    private SessionService sessionService; // fake session service (controller needs it for cookies)
    @MockitoBean
    private SellerService sellerService; // fake seller service (controller needs it for seller registration)

    // ---- Test 1: Successfully register a buyer ----
    @Test
    void registerBuyer_returnsUserData() throws Exception {
        User mockUser = new User(); // create fake user
        mockUser.setId(999L); // fake ID (999, L = long type)
        mockUser.setEmail("buyer@example.com");
        mockUser.setRole(User.Role.BUYER);
        mockUser.setUsername("buyeruser");
        mockUser.setFirstName("Zelda");
        mockUser.setLastName("Link");
        mockUser.setPhoneNumber("555-1234");
        mockUser.setProfilePicture(null);

        Session mockSession = new Session(); // fake session for cookie
        mockSession.setSessionId("fake-session-id");

        when(authService.register("buyer@example.com", "password123", User.Role.BUYER, // when register() is called with these values...
                "buyeruser", "Zelda", "Link", "555-1234", null))
                .thenReturn(mockUser); // ...return our fake user
        when(sessionService.createSession(any(User.class))).thenReturn(mockSession); // return fake session for any user

        mockMvc.perform(post("/api/auth/register/buyer") // send POST to buyer registration
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"buyer@example.com\", \"password\": \"password123\", "
                                + "\"username\": \"buyeruser\", \"firstName\": \"Zelda\", "
                                + "\"lastName\": \"Link\", \"phoneNumber\": \"555-1234\"}"))
                .andExpect(status().isOk()) // expect 200
                .andExpect(jsonPath("$.email").value("buyer@example.com")) // verify email
                .andExpect(jsonPath("$.role").value("BUYER")) // verify role
                .andExpect(jsonPath("$.username").value("buyeruser")) // verify username
                .andExpect(jsonPath("$.firstName").value("Zelda")) // verify first name
                .andExpect(jsonPath("$.lastName").value("Link")); // verify last name
    }

    // ---- Test 2: Successfully register a seller ----
    @Test
    void registerSeller_returnsUserData() throws Exception {
        User mockUser = new User();
        mockUser.setId(299L);
        mockUser.setEmail("seller@example.com");
        mockUser.setRole(User.Role.SELLER);
        mockUser.setUsername("selleruser");
        mockUser.setFirstName("Ganondorf");
        mockUser.setLastName("Ganon");
        mockUser.setPhoneNumber("555-5678");
        mockUser.setProfilePicture(null);

        Session mockSession = new Session();
        mockSession.setSessionId("fake-session-id");

        when(authService.register("seller@example.com", "password123", User.Role.SELLER,
                "selleruser", "Ganondorf", "Ganon", "555-5678", null))
                .thenReturn(mockUser);
        when(sessionService.createSession(any(User.class))).thenReturn(mockSession);

        mockMvc.perform(post("/api/auth/register/seller") // send POST to seller registration
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"seller@example.com\", \"password\": \"password123\", "
                                + "\"username\": \"selleruser\", \"firstName\": \"Ganondorf\", "
                                + "\"lastName\": \"Ganon\", \"phoneNumber\": \"555-5678\"}"))
                .andExpect(status().isOk()) // expect 200
                .andExpect(jsonPath("$.email").value("seller@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.username").value("selleruser"))
                .andExpect(jsonPath("$.firstName").value("Ganondorf"))
                .andExpect(jsonPath("$.lastName").value("Ganon"));
    }

    // ---- Test 3: Registration fails when email is already taken ----
    @Test
    void registerWithExistingEmail_returns400() throws Exception {
        when(authService.register(eq("taken@example.com"), eq("password123"), eq(User.Role.BUYER), // when called with this email...
                any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Email already registered")); // ...throw duplicate email error

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"taken@example.com\", \"password\": \"password123\", "
                                + "\"username\": \"someone\", \"firstName\": \"A\", "
                                + "\"lastName\": \"B\", \"phoneNumber\": \"555-0000\"}"))
                .andExpect(status().isBadRequest()) // expect 400
                .andExpect(jsonPath("$.error").value("Email already registered")); // verify error message in JSON
    }
}
