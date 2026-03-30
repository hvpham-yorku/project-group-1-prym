package com.prym.backend.unit.controller;

import com.prym.backend.controller.AuthController;
import com.prym.backend.model.Session;
import com.prym.backend.model.User;
import com.prym.backend.service.AuthService;
import com.prym.backend.service.SellerService;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Extended AuthController tests covering buyer/seller registration and the updateUserInfo endpoint.
 * LoginTest.java already covers /login, /logout, and /me — this file covers the remaining endpoints.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerExtendedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private SellerService sellerService;

    // ─── Buyer Registration ────────────────────────────────────────────

    // Test 1: registerBuyer_Success — all fields provided → 200 with user JSON + session cookie set
    @Test
    void registerBuyer_Success_Returns200WithUserData() throws Exception {
        User mockUser = buildUser(1L, "jane@example.com", "janedoe", User.Role.BUYER);
        Session mockSession = buildSession("session-abc");

        when(authService.register(
                eq("jane@example.com"), eq("pass123"), eq(User.Role.BUYER),
                eq("janedoe"), eq("Jane"), eq("Doe"), eq("416-555-0001"),
                isNull(), eq("10001")))
                .thenReturn(mockUser);
        when(sessionService.createSession(any(User.class))).thenReturn(mockSession);

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":       "jane@example.com",
                              "password":    "pass123",
                              "username":    "janedoe",
                              "firstName":   "Jane",
                              "lastName":    "Doe",
                              "phoneNumber": "416-555-0001",
                              "zipCode":     "10001"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andExpect(jsonPath("$.username").value("janedoe"))
                .andExpect(jsonPath("$.id").value(1));
    }

    // Test 2: registerBuyer_DuplicateEmail — service throws "Email already registered" → 400
    @Test
    void registerBuyer_DuplicateEmail_Returns400() throws Exception {
        when(authService.register(anyString(), anyString(), eq(User.Role.BUYER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Email already registered"));

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":       "dup@example.com",
                              "password":    "pass123",
                              "username":    "newuser",
                              "firstName":   "New",
                              "lastName":    "User",
                              "phoneNumber": "416-000-0000",
                              "zipCode":     "10001"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    // Test 3: registerBuyer_DuplicateUsername — service throws "Username already taken" → 400
    @Test
    void registerBuyer_DuplicateUsername_Returns400() throws Exception {
        when(authService.register(anyString(), anyString(), eq(User.Role.BUYER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Username already taken"));

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":     "unique@example.com",
                              "password":  "pass",
                              "username":  "taken",
                              "firstName": "A",
                              "lastName":  "B",
                              "phoneNumber": "416-000-0000",
                              "zipCode":   "10001"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    // Test 4: registerBuyer_SetsSessionCookie — response must include Set-Cookie header with SESSION_ID
    @Test
    void registerBuyer_SetsSessionCookieInResponse() throws Exception {
        User mockUser = buildUser(2L, "cookie@example.com", "cookieuser", User.Role.BUYER);
        Session mockSession = buildSession("test-session-xyz");

        when(authService.register(anyString(), anyString(), eq(User.Role.BUYER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(mockUser);
        when(sessionService.createSession(any(User.class))).thenReturn(mockSession);

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":       "cookie@example.com",
                              "password":    "pass",
                              "username":    "cookieuser",
                              "firstName":   "Cookie",
                              "lastName":    "User",
                              "phoneNumber": "416-000-0001",
                              "zipCode":     "10001"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    // Test 5: registerBuyer_ResponseContainsAllUserFields — all 12 expected fields are present
    @Test
    void registerBuyer_ResponseContainsAllExpectedFields() throws Exception {
        User mockUser = buildUser(3L, "full@example.com", "fulluser", User.Role.BUYER);
        mockUser.setFirstName("Full");
        mockUser.setLastName("User");
        mockUser.setPhoneNumber("416-999-0000");
        mockUser.setZipCode("10001");
        mockUser.setCity("New York");
        mockUser.setState("NY");
        mockUser.setCountry("United States");

        when(authService.register(anyString(), anyString(), eq(User.Role.BUYER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(mockUser);
        when(sessionService.createSession(any(User.class))).thenReturn(buildSession("s1"));

        mockMvc.perform(post("/api/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"full@example.com\",\"password\":\"p\",\"username\":\"fulluser\",\"firstName\":\"Full\",\"lastName\":\"User\",\"phoneNumber\":\"416-999-0000\",\"zipCode\":\"10001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andExpect(jsonPath("$.phoneNumber").exists())
                .andExpect(jsonPath("$.zipCode").exists())
                .andExpect(jsonPath("$.city").exists())
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.country").exists());
    }

    // ─── Seller Registration ───────────────────────────────────────────

    // Test 6: registerSeller_Success — all fields provided → 200, seller profile auto-created
    @Test
    void registerSeller_Success_Returns200AndCreatesSellerProfile() throws Exception {
        User mockSeller = buildUser(10L, "farm@example.com", "greenfarmer", User.Role.SELLER);
        Session mockSession = buildSession("seller-session-1");

        when(authService.register(
                eq("farm@example.com"), eq("farmpass"), eq(User.Role.SELLER),
                eq("greenfarmer"), eq("John"), eq("Farmer"), eq("519-555-1111"),
                isNull(), eq("N2L5V9")))
                .thenReturn(mockSeller);
        when(sessionService.createSession(any(User.class))).thenReturn(mockSession);

        mockMvc.perform(post("/api/auth/register/seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":       "farm@example.com",
                              "password":    "farmpass",
                              "username":    "greenfarmer",
                              "firstName":   "John",
                              "lastName":    "Farmer",
                              "phoneNumber": "519-555-1111",
                              "shopName":    "Green Acres Farm",
                              "zipCode":     "N2L5V9"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("farm@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"));

        // Seller profile must be auto-created with the provided shopName
        verify(sellerService).createSellerProfile(eq(10L), eq("Green Acres Farm"), eq(""), eq(""));
    }

    // Test 7: registerSeller_DuplicateEmail — service throws → 400 with error message
    @Test
    void registerSeller_DuplicateEmail_Returns400() throws Exception {
        when(authService.register(anyString(), anyString(), eq(User.Role.SELLER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Email already registered"));

        mockMvc.perform(post("/api/auth/register/seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@farm.com\",\"password\":\"p\",\"username\":\"u\",\"firstName\":\"F\",\"lastName\":\"L\",\"phoneNumber\":\"000\",\"shopName\":\"Farm\",\"zipCode\":\"10001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered"));

        // Seller profile must NOT be created when registration fails
        verify(sellerService, never()).createSellerProfile(any(), any(), any(), any());
    }

    // Test 8: registerSeller_DuplicateUsername → 400
    @Test
    void registerSeller_DuplicateUsername_Returns400() throws Exception {
        when(authService.register(anyString(), anyString(), eq(User.Role.SELLER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Username already taken"));

        mockMvc.perform(post("/api/auth/register/seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@farm.com\",\"password\":\"p\",\"username\":\"taken\",\"firstName\":\"F\",\"lastName\":\"L\",\"phoneNumber\":\"000\",\"shopName\":\"Farm\",\"zipCode\":\"10001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    // Test 9: registerSeller_NullShopName — shopName absent in body → still registers, profile created with null
    @Test
    void registerSeller_NullShopName_ProfileCreatedWithNull() throws Exception {
        User mockSeller = buildUser(11L, "noname@farm.com", "noname_farmer", User.Role.SELLER);
        when(authService.register(anyString(), anyString(), eq(User.Role.SELLER),
                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(mockSeller);
        when(sessionService.createSession(any(User.class))).thenReturn(buildSession("s2"));

        mockMvc.perform(post("/api/auth/register/seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noname@farm.com\",\"password\":\"p\",\"username\":\"noname_farmer\",\"firstName\":\"F\",\"lastName\":\"L\",\"phoneNumber\":\"000\",\"zipCode\":\"10001\"}"))
                .andExpect(status().isOk());

        // shopName will be null because it wasn't in the request body
        verify(sellerService).createSellerProfile(eq(11L), isNull(), eq(""), eq(""));
    }

    // ─── updateUserInfo ────────────────────────────────────────────────

    // Test 10: updateUserInfo_Success — valid session + valid body → 200 with updated user
    @Test
    void updateUserInfo_Success_Returns200WithUpdatedUser() throws Exception {
        User existingUser = buildUser(1L, "old@example.com", "olduser", User.Role.BUYER);
        User updatedUser  = buildUser(1L, "new@example.com", "newuser", User.Role.BUYER);
        updatedUser.setFirstName("NewFirst");
        updatedUser.setLastName("NewLast");

        when(sessionService.validateSession("valid-session")).thenReturn(Optional.of(existingUser));
        when(authService.updateUserInfo(eq(1L), eq("NewFirst"), eq("NewLast"),
                eq("new@example.com"), eq("newuser"), isNull(), isNull(), isNull()))
                .thenReturn(updatedUser);

        mockMvc.perform(patch("/api/auth/user")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "valid-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "firstName": "NewFirst",
                              "lastName":  "NewLast",
                              "email":     "new@example.com",
                              "username":  "newuser"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.firstName").value("NewFirst"))
                .andExpect(jsonPath("$.lastName").value("NewLast"));
    }

    // Test 11: updateUserInfo_NoSessionCookie — missing SESSION_ID cookie → 401
    @Test
    void updateUserInfo_NoSessionCookie_Returns401() throws Exception {
        mockMvc.perform(patch("/api/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not logged in"));
    }

    // Test 12: updateUserInfo_ExpiredSession — validateSession returns empty → 401
    @Test
    void updateUserInfo_ExpiredSession_Returns401() throws Exception {
        when(sessionService.validateSession("expired-sess")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/auth/user")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "expired-sess"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Session expired"));
    }

    // Test 13: updateUserInfo_EmailAlreadyInUse — service throws → 400 with error
    @Test
    void updateUserInfo_EmailAlreadyInUse_Returns400() throws Exception {
        User currentUser = buildUser(1L, "current@example.com", "currentuser", User.Role.BUYER);
        when(sessionService.validateSession("sess")).thenReturn(Optional.of(currentUser));
        when(authService.updateUserInfo(anyLong(), anyString(), anyString(), anyString(),
                anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("Email already in use"));

        mockMvc.perform(patch("/api/auth/user")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "sess"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@example.com\",\"username\":\"currentuser\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    // Test 14: updateUserInfo_UsernameAlreadyTaken — service throws → 400 with error
    @Test
    void updateUserInfo_UsernameAlreadyTaken_Returns400() throws Exception {
        User currentUser = buildUser(1L, "me@example.com", "me", User.Role.BUYER);
        when(sessionService.validateSession("sess2")).thenReturn(Optional.of(currentUser));
        when(authService.updateUserInfo(anyLong(), anyString(), anyString(), anyString(),
                anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("Username already taken"));

        mockMvc.perform(patch("/api/auth/user")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "sess2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"me@example.com\",\"username\":\"taken\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    // Test 15: updateUserInfo_WithProfilePictureAndZipCode — all optional fields forwarded to service
    @Test
    void updateUserInfo_WithProfilePictureAndZipCode_ForwardsToService() throws Exception {
        User currentUser = buildUser(5L, "pic@example.com", "picuser", User.Role.BUYER);
        User updatedUser = buildUser(5L, "pic@example.com", "picuser", User.Role.BUYER);
        updatedUser.setZipCode("M5V2T6");

        when(sessionService.validateSession("pic-sess")).thenReturn(Optional.of(currentUser));
        when(authService.updateUserInfo(eq(5L), anyString(), anyString(), anyString(),
                anyString(), isNull(), eq("https://img.example.com/pic.jpg"), eq("M5V2T6")))
                .thenReturn(updatedUser);

        mockMvc.perform(patch("/api/auth/user")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "pic-sess"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "firstName":      "Pic",
                              "lastName":       "User",
                              "email":          "pic@example.com",
                              "username":       "picuser",
                              "profilePicture": "https://img.example.com/pic.jpg",
                              "zipCode":        "M5V2T6"
                            }
                            """))
                .andExpect(status().isOk());

        verify(authService).updateUserInfo(eq(5L), anyString(), anyString(), anyString(),
                anyString(), isNull(), eq("https://img.example.com/pic.jpg"), eq("M5V2T6"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private User buildUser(Long id, String email, String username, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setUsername(username);
        u.setRole(role);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setPhoneNumber("000-000-0000");
        return u;
    }

    private Session buildSession(String sessionId) {
        Session s = new Session();
        s.setSessionId(sessionId);
        return s;
    }
}
