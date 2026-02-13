package com.prym.backend.controller;

import com.prym.backend.model.User; //Imports user modal for creation of fake User objects
import com.prym.backend.service.AuthService; //Imports AuthService for creation of mock version of it
import org.junit.jupiter.api.Test; //So that we can use @Test to make JUnit tests run
import org.springframework.beans.factory.annotation.Autowired; //Imports @Autowired which tells Spring to automatically inject the MockMvc instance
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; //Imports @AutoConfigureMockMvc which configures MockMvc; addFilters=False disables Spring Security for testing 
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest; //Imports @WebMvcTest which tells Spring to only load the web layer (just controller not whole app)
import org.springframework.http.MediaType; //Imports MediaType which is used to tell the request that we're sending JSON data
import org.springframework.test.context.bean.override.mockito.MockitoBean; //Imports @MockitoBean which creates a fake version of a class and puts it in Spring's context
import org.springframework.test.web.servlet.MockMvc; //Imports MockMvc, a tool that simulates HTTP requests without having a real running server

import java.util.Optional; //Imports Optional, a Java wrapper that can hold a value or be empty (used by AuthService.login)

import static org.mockito.ArgumentMatchers.anyString; //Imports anyString(), a Mockito matcher that means "any string value at all"
import static org.mockito.Mockito.when; //Imports when(), used to tell the mock "when this method is called, do this"
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; //Imports post(), which builds a fake POST request to send via MockMvc
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; //Imports status(), content(), jsonPath(), used to check the HTTP response

// @WebMvcTest loads ONLY the AuthController (not the full app)
// @AutoConfigureMockMvc(addFilters = false) skips Spring Security filters so we can
// test the controller logic without needing authentication setup.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginTest {

    // String automatically provides a MockMvc instance here (our fake HTTP client)
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean creates a fake version of AuthService so we can control what it returns instead of hitting real db
    @MockitoBean
    private AuthService authService;

    // ---- Test 1: Successful login with correct credentials ----
    @Test
    void loginWithValidCredentials_returnsUserData() throws Exception {
        //Create a brand new User object
        User mockUser = new User();
        mockUser.setId(999L); //Fake ID (999 of type long)
        mockUser.setEmail("test@example.com");
        mockUser.setRole(User.Role.BUYER);

        //Tell the mock that when login() is called with these exact credentials, return the user
        when(authService.login("test@example.com", "password123"))
                .thenReturn(Optional.of(mockUser));

        //Send a fake POST request to /api/auth/login and verify the response
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", \"password\": \"password123\"}"))
                .andExpect(status().isOk())                          // expect HTTP status 200 OK
                .andExpect(jsonPath("$.email").value("test@example.com")) // check that email field is the same in JSON response
                .andExpect(jsonPath("$.role").value("BUYER"));           // check role is BUYER in JSON
    }

    // ---- Test 2: Failed login with wrong credentials ----
    @Test
    void loginWithInvalidCredentials_returns401() throws Exception {
        //The mock returns empty (meaning no user found / wrong password)
        when(authService.login(anyString(), anyString()))
                .thenReturn(Optional.empty());

        //Send a login request and expect a 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"wrong@example.com\", \"password\": \"wrongpass\"}"))
                .andExpect(status().isUnauthorized())                        // expect HTTP status 401 Unauthorized
                .andExpect(content().string("Invalid email or password"));   // check error message
    }

    // ---- Test 3: Login with empty/missing fields ----
    @Test
    void loginWithEmptyBody_returns401() throws Exception {
        //When email and password are null (missing from JSON), service returns empty
        when(authService.login(null, null))
                .thenReturn(Optional.empty()); //Return empty, aka login failed

        //Send an empty JSON body and expect 401, not a server crash
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) //Empty JSON object; both fields should be null in the controller
                .andExpect(status().isUnauthorized()); // Expect 401, NOT a 500 server error; the app handles it gracefully
    }
}
