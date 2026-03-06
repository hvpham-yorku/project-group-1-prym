package com.prym.backend.controller;

import com.prym.backend.model.Cow;
import com.prym.backend.model.CowCut;
import com.prym.backend.model.User;
import com.prym.backend.service.CowService;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CowControllerTest {

    @Mock
    private CowService cowService;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private CowController cowController;

    private User testUser;
    private Cow testCow;
    private String validSessionId = "valid-session-id";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(User.Role.SELLER);

        testCow = new Cow();
        testCow.setId(1L);
        testCow.setName("Lot #1");
        testCow.setEstimatedWeightLbs(800.0);
        testCow.setStatus(Cow.CowStatus.OPEN);

        when(sessionService.validateSession(validSessionId)).thenReturn(Optional.of(testUser));
    }

    // Test 1: createCow_Success
    @Test
    void createCow_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("cowTypeId", "1");
        request.put("name", "Lot #1");
        request.put("estimatedWeightLbs", "800.0");
        request.put("harvestDate", "2026-06-01");

        when(cowService.createCow(1L, "Lot #1", 800.0, LocalDate.of(2026, 6, 1)))
                .thenReturn(testCow);

        ResponseEntity<?> response = cowController.createCow(1L, request, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testCow, response.getBody());
    }

    // Test 2: createCow_Forbidden
    @Test
    void createCow_Forbidden() {
        Map<String, Object> request = new HashMap<>();
        request.put("cowTypeId", "1");
        request.put("name", "Lot #1");

        // userId 2 does not match session user (id 1)
        ResponseEntity<?> response = cowController.createCow(2L, request, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 3: createCow_ServiceError
    @Test
    void createCow_ServiceError() {
        Map<String, Object> request = new HashMap<>();
        request.put("cowTypeId", "999");
        request.put("name", "Lot #1");
        request.put("estimatedWeightLbs", "800.0");
        request.put("harvestDate", "2026-06-01");

        when(cowService.createCow(999L, "Lot #1", 800.0, LocalDate.of(2026, 6, 1)))
                .thenThrow(new RuntimeException("CowType not found"));

        ResponseEntity<?> response = cowController.createCow(1L, request, validSessionId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("CowType not found", body.get("error"));
    }

    // Test 4: getCows_Success
    @Test
    void getCows_Success() {
        when(cowService.getCowsBySeller(1L)).thenReturn(List.of(testCow));

        ResponseEntity<?> response = cowController.getCows(1L, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> body = (List<?>) response.getBody();
        assertEquals(1, body.size());
    }

    // Test 5: getCows_Forbidden
    @Test
    void getCows_Forbidden() {
        ResponseEntity<?> response = cowController.getCows(2L, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 6: getAllCuts_Success
    @Test
    void getAllCuts_Success() {
        CowCut cut1 = new CowCut();
        cut1.setStatus(CowCut.CutStatus.AVAILABLE);
        CowCut cut2 = new CowCut();
        cut2.setStatus(CowCut.CutStatus.CLAIMED);

        when(cowService.getAllCuts(1L)).thenReturn(List.of(cut1, cut2));

        ResponseEntity<?> response = cowController.getAllCuts(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> body = (List<?>) response.getBody();
        assertEquals(2, body.size());
    }

    // Test 7: getAvailableCuts_Success
    @Test
    void getAvailableCuts_Success() {
        CowCut availableCut = new CowCut();
        availableCut.setStatus(CowCut.CutStatus.AVAILABLE);

        when(cowService.getAvailableCuts(1L)).thenReturn(List.of(availableCut));

        ResponseEntity<?> response = cowController.getAvailableCuts(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> body = (List<?>) response.getBody();
        assertEquals(1, body.size());
    }
}
