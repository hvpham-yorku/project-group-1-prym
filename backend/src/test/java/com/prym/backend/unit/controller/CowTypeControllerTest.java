package com.prym.backend.unit.controller;
import com.prym.backend.controller.CowTypeController;

import com.prym.backend.model.CowType;
import com.prym.backend.model.User;
import com.prym.backend.service.CowTypeService;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CowTypeControllerTest {

    @Mock
    private CowTypeService cowTypeService;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private CowTypeController cowTypeController;

    private User testUser;
    private CowType testCowType;
    private String validSessionId = "valid-session-id";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(User.Role.SELLER);

        testCowType = new CowType();
        testCowType.setId(1L);
        testCowType.setBreed(CowType.Breed.ANGUS);
        testCowType.setDescription("Black Angus beef");
        testCowType.setPricePerPound(8.99);
        testCowType.setAvailableCount(5);

        when(sessionService.validateSession(validSessionId)).thenReturn(Optional.of(testUser));
    }

    // Test 1: createCowType_Success
    @Test
    void createCowType_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("breed", "Angus");
        request.put("description", "Black Angus beef");
        request.put("pricePerPound", "8.99");
        request.put("availableCount", "5");

        when(cowTypeService.createCowType(1L, "Angus", "Black Angus beef", 8.99, 5))
                .thenReturn(testCowType);

        ResponseEntity<?> response = cowTypeController.createCowType(1L, request, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testCowType, response.getBody());
    }

    // Test 2: createCowType_Forbidden (userId mismatch)
    @Test
    void createCowType_Forbidden() {
        Map<String, Object> request = new HashMap<>();
        request.put("breed", "Angus");

        // userId 2 does not match session user (id 1)
        ResponseEntity<?> response = cowTypeController.createCowType(2L, request, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Access denied", body.get("error"));
    }

    // Test 3: createCowType_ServiceError (invalid breed)
    @Test
    void createCowType_ServiceError() {
        Map<String, Object> request = new HashMap<>();
        request.put("breed", "InvalidBreed");
        request.put("description", "Bad breed");
        request.put("pricePerPound", "5.00");
        request.put("availableCount", "1");

        when(cowTypeService.createCowType(1L, "InvalidBreed", "Bad breed", 5.00, 1))
                .thenThrow(new RuntimeException("Invalid breed"));

        ResponseEntity<?> response = cowTypeController.createCowType(1L, request, validSessionId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Invalid breed", body.get("error"));
    }

    // Test 4: getCowTypes_Success
    @Test
    void getCowTypes_Success() {
        when(cowTypeService.getCowTypesBySeller(1L)).thenReturn(List.of(testCowType));

        ResponseEntity<?> response = cowTypeController.getCowTypes(1L, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> body = (List<?>) response.getBody();
        assertEquals(1, body.size());
    }

    // Test 5: getCowTypes_Forbidden
    @Test
    void getCowTypes_Forbidden() {
        ResponseEntity<?> response = cowTypeController.getCowTypes(2L, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 6: getCowTypes_SellerNotFound
    @Test
    void getCowTypes_SellerNotFound() {
        when(cowTypeService.getCowTypesBySeller(1L))
                .thenThrow(new RuntimeException("Seller not found"));

        ResponseEntity<?> response = cowTypeController.getCowTypes(1L, validSessionId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Seller not found", body.get("error"));
    }

    // Test 7: updateCowType_Success
    @Test
    void updateCowType_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("breed", "Wagyu");
        request.put("description", "Premium Wagyu");
        request.put("pricePerPound", "25.00");
        request.put("availableCount", "3");

        CowType updated = new CowType();
        updated.setId(1L);
        updated.setBreed(CowType.Breed.WAGYU);
        updated.setDescription("Premium Wagyu");
        updated.setPricePerPound(25.00);
        updated.setAvailableCount(3);

        when(cowTypeService.updateCowType(1L, "Wagyu", "Premium Wagyu", 25.00, 3))
                .thenReturn(updated);

        ResponseEntity<?> response = cowTypeController.updateCowType(1L, 1L, request, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
    }

    // Test 8: updateCowType_Forbidden
    @Test
    void updateCowType_Forbidden() {
        Map<String, Object> request = new HashMap<>();
        request.put("breed", "Wagyu");

        ResponseEntity<?> response = cowTypeController.updateCowType(2L, 1L, request, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 9: updateCowType_NotFound
    @Test
    void updateCowType_NotFound() {
        Map<String, Object> request = new HashMap<>();
        request.put("breed", "Wagyu");
        request.put("description", null);
        request.put("pricePerPound", null);
        request.put("availableCount", null);

        when(cowTypeService.updateCowType(1L, "Wagyu", null, null, null))
                .thenThrow(new RuntimeException("CowType not found"));

        ResponseEntity<?> response = cowTypeController.updateCowType(1L, 1L, request, validSessionId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("CowType not found", body.get("error"));
    }

    // Test 10: deleteCowType_Success
    @Test
    void deleteCowType_Success() {
        doNothing().when(cowTypeService).deleteCowType(1L);

        ResponseEntity<?> response = cowTypeController.deleteCowType(1L, 1L, validSessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("CowType deleted", body.get("message"));
        verify(cowTypeService).deleteCowType(1L);
    }

    // Test 11: deleteCowType_Forbidden
    @Test
    void deleteCowType_Forbidden() {
        ResponseEntity<?> response = cowTypeController.deleteCowType(2L, 1L, validSessionId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(cowTypeService, never()).deleteCowType(anyLong());
    }

    // Test 12: deleteCowType_NotFound
    @Test
    void deleteCowType_NotFound() {
        doThrow(new RuntimeException("CowType not found")).when(cowTypeService).deleteCowType(99L);

        ResponseEntity<?> response = cowTypeController.deleteCowType(1L, 99L, validSessionId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("CowType not found", body.get("error"));
    }
}
