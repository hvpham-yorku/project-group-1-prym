package com.prym.backend.unit.controller;
import com.prym.backend.controller.GroupController;

import com.prym.backend.model.User;
import com.prym.backend.repository.UserRepository;
import com.prym.backend.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupController groupController;

    private User testUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("buyer@example.com");
        testUser.setRole(User.Role.BUYER);

        // Simulate a logged-in user via Spring Security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer@example.com", null));
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
    }

    // Test 1: createGroup_Success — logged-in user matches userId → group created
    @Test
    public void createGroup_Success() {
        Map<String, Object> resultDTO = Map.of("groupId", 100L, "groupName", "Test Group");
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);
        body.put("name", "Test Group");
        body.put("certifications", "HALAL");

        when(groupService.createGroup(1L, "Test Group", "HALAL")).thenReturn(resultDTO);

        ResponseEntity<?> response = groupController.createGroup(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Test Group", responseBody.get("groupName"));
    }

    // Test 2: createGroup_Forbidden — userId in body doesn't match logged-in user
    @Test
    public void createGroup_Forbidden() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 99); // different from logged-in user ID (1)
        body.put("name", "Test Group");

        ResponseEntity<?> response = groupController.createGroup(body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Access denied", responseBody.get("error"));
    }

    // Test 3: createGroup_BlankName — service throws for blank name → 400
    @Test
    public void createGroup_BlankName() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);
        body.put("name", "  ");

        when(groupService.createGroup(1L, "  ", ""))
                .thenThrow(new RuntimeException("Group name is required."));

        ResponseEntity<?> response = groupController.createGroup(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Group name is required.", responseBody.get("error"));
    }

    // Test 4: createGroup_AlreadyInGroup — service throws for existing membership → 400
    @Test
    public void createGroup_AlreadyInGroup() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);
        body.put("name", "New Group");

        when(groupService.createGroup(1L, "New Group", ""))
                .thenThrow(new RuntimeException("You are already in a group. Leave your current group before creating a new one."));

        ResponseEntity<?> response = groupController.createGroup(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("already in a group"));
    }

    // Test 5: getAvailableGroups_Success — returns groups buyer has not joined
    @Test
    public void getAvailableGroups_Success() {
        when(groupService.getAvailableGroups(1L)).thenReturn(List.of(Map.of("groupId", 200L)));

        ResponseEntity<?> response = groupController.getAvailableGroups(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 6: getAvailableGroups_Forbidden — userId param doesn't match logged-in user
    @Test
    public void getAvailableGroups_Forbidden() {
        ResponseEntity<?> response = groupController.getAvailableGroups(99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Access denied", responseBody.get("error"));
    }

    // Test 7: getMyGroups_Success — returns groups the buyer is already in
    @Test
    public void getMyGroups_Success() {
        when(groupService.getMyGroups(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = groupController.getMyGroups(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 8: getMyGroups_Forbidden — userId param doesn't match logged-in user
    @Test
    public void getMyGroups_Forbidden() {
        ResponseEntity<?> response = groupController.getMyGroups(99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 9: getGroup_Success — valid userId and groupId → returns group details
    @Test
    public void getGroup_Success() {
        when(groupService.getGroup(1L, 100L)).thenReturn(Map.of("groupId", 100L));

        ResponseEntity<?> response = groupController.getGroup(100L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 10: getGroup_Forbidden — userId param doesn't match logged-in user
    @Test
    public void getGroup_Forbidden() {
        ResponseEntity<?> response = groupController.getGroup(100L, 99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 11: joinGroup_Success — buyer joins an available group
    @Test
    public void joinGroup_Success() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);

        when(groupService.joinGroup(1L, 100L)).thenReturn(Map.of("groupId", 100L));

        ResponseEntity<?> response = groupController.joinGroup(100L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 12: joinGroup_Forbidden — userId in body doesn't match logged-in user
    @Test
    public void joinGroup_Forbidden() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 99); // different from logged-in user ID (1)

        ResponseEntity<?> response = groupController.joinGroup(100L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 13: joinGroup_AlreadyInGroup — service throws for existing membership → 400
    @Test
    public void joinGroup_AlreadyInGroup() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);

        when(groupService.joinGroup(1L, 100L)).thenThrow(
                new RuntimeException("You are already in a group. Leave your current group before joining another."));

        ResponseEntity<?> response = groupController.joinGroup(100L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("already in a group"));
    }

    // Test 14: saveCuts_Success — valid cuts within available slots → saved
    @Test
    public void saveCuts_Success() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);
        body.put("cuts", "Chuck, Rib");

        when(groupService.saveCuts(1L, 100L, "Chuck, Rib")).thenReturn(Map.of("groupId", 100L));

        ResponseEntity<?> response = groupController.saveCuts(100L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 15: saveCuts_Forbidden — userId in body doesn't match logged-in user
    @Test
    public void saveCuts_Forbidden() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 99);
        body.put("cuts", "Chuck");

        ResponseEntity<?> response = groupController.saveCuts(100L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 16: saveCuts_SlotsFull — service throws because slots are taken → 400
    @Test
    public void saveCuts_SlotsFull() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);
        body.put("cuts", "Chuck");

        when(groupService.saveCuts(1L, 100L, "Chuck"))
                .thenThrow(new RuntimeException("'Chuck' only has 0 slot(s) left."));

        ResponseEntity<?> response = groupController.saveCuts(100L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertTrue(responseBody.get("error").toString().contains("slot(s) left"));
    }

    // Test 17: leaveGroup_Success — buyer leaves their group → 200 with message
    @Test
    public void leaveGroup_Success() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);

        ResponseEntity<?> response = groupController.leaveGroup(100L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Left group successfully", responseBody.get("message"));
        verify(groupService).leaveGroup(1L, 100L);
    }

    // Test 18: leaveGroup_Forbidden — userId in body doesn't match logged-in user
    @Test
    public void leaveGroup_Forbidden() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 99);

        ResponseEntity<?> response = groupController.leaveGroup(100L, body);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 19: leaveGroup_NotMember — service throws because buyer is not a member → 400
    @Test
    public void leaveGroup_NotMember() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1);

        doThrow(new RuntimeException("You are not a member of this group."))
                .when(groupService).leaveGroup(1L, 100L);

        ResponseEntity<?> response = groupController.leaveGroup(100L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("You are not a member of this group.", responseBody.get("error"));
    }


     // Test 26: getMatchingFarms_Success — returns matching farms DTO
    @Test
    public void getMatchingFarms_Success() {
        Map<String, Object> dto = Map.of(
                "perfectMatches", List.of(),
                "partialMatches", List.of());
        when(groupService.getMatchingFarms(1L, 100L)).thenReturn(dto);

        ResponseEntity<?> response = groupController.getMatchingFarms(100L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Test 27: getMatchingFarms_Forbidden — userId param doesn't match logged-in user
    @Test
    public void getMatchingFarms_Forbidden() {
        ResponseEntity<?> response = groupController.getMatchingFarms(100L, 99L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
