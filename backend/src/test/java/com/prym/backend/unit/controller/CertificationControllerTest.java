package com.prym.backend.unit.controller;
import com.prym.backend.controller.CertificationController;

import com.prym.backend.model.Certification;
import com.prym.backend.model.User;
import com.prym.backend.service.CertificationService;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Loads only CertificationController — not the full Spring application
@WebMvcTest(CertificationController.class)
// Disables Spring Security filters so we can test controller logic without authentication setup
@AutoConfigureMockMvc(addFilters = false)
class CertificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificationService certificationService;

    @MockitoBean
    private SessionService sessionService;

    // Helper: creates a fake User with the given ID
    private User makeUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("seller@example.com");
        u.setRole(User.Role.SELLER);
        return u;
    }

    // Test 1: addCertification_Success — valid session + matching userId returns 200
    @Test
    void addCertification_Success() throws Exception {
        User user = makeUser(1L);
        Certification cert = new Certification();
        cert.setName(Certification.CertificationType.HALAL);

        when(sessionService.validateSession("valid-session")).thenReturn(Optional.of(user));
        when(certificationService.addCertification(eq(1L), eq("HALAL"), eq("USDA"), any()))
                .thenReturn(cert);

        mockMvc.perform(post("/api/seller/certifications/1")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "valid-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"HALAL\", \"issuingBody\": \"USDA\", \"expiryDate\": \"2027-01-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("HALAL"));
    }

    // Test 2: addCertification_InvalidSession — no matching session returns 403
    @Test
    void addCertification_InvalidSession() throws Exception {
        when(sessionService.validateSession("bad-session")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/seller/certifications/1")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"HALAL\", \"issuingBody\": \"USDA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    // Test 3: addCertification_WrongUser — session belongs to a different user than the path userId
    @Test
    void addCertification_WrongUser() throws Exception {
        User user = makeUser(99L); // logged-in user is 99, but path says userId=1
        when(sessionService.validateSession("valid-session")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/seller/certifications/1")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "valid-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"HALAL\", \"issuingBody\": \"USDA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    // Test 4: addCertification_ServiceError — service throws returns 400
    @Test
    void addCertification_ServiceError() throws Exception {
        User user = makeUser(1L);
        when(sessionService.validateSession("valid-session")).thenReturn(Optional.of(user));
        when(certificationService.addCertification(eq(1L), any(), any(), any()))
                .thenThrow(new RuntimeException("Seller not found"));

        mockMvc.perform(post("/api/seller/certifications/1")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "valid-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"HALAL\", \"issuingBody\": \"USDA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Seller not found"));
    }

    // Test 5: getCertifications_Success — returns list of certifications
    @Test
    void getCertifications_Success() throws Exception {
        User user = makeUser(1L);
        Certification cert = new Certification();
        cert.setName(Certification.CertificationType.ORGANIC);

        when(sessionService.validateSession("valid-session")).thenReturn(Optional.of(user));
        when(certificationService.getCertificationsBySeller(1L)).thenReturn(List.of(cert));

        mockMvc.perform(get("/api/seller/certifications/1")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "valid-session")))
                .andExpect(status().isOk());
    }

    // Test 6: getCertifications_InvalidSession — no session returns 403
    @Test
    void getCertifications_InvalidSession() throws Exception {
        when(sessionService.validateSession("bad-session")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/seller/certifications/1")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "bad-session")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    // Test 7: deleteCertification_Success — valid session deletes cert and returns message
    @Test
    void deleteCertification_Success() throws Exception {
        User user = makeUser(1L);
        when(sessionService.validateSession("valid-session")).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/seller/certifications/1/5")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "valid-session")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Certification deleted"));

        verify(certificationService).deleteCertification(5L);
    }

    // Test 8: deleteCertification_InvalidSession — no session returns 403
    @Test
    void deleteCertification_InvalidSession() throws Exception {
        when(sessionService.validateSession("bad-session")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/seller/certifications/1/5")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", "bad-session")))
                .andExpect(status().isForbidden());
    }
}
