package com.prym.backend.service;

import com.prym.backend.model.Session;
import com.prym.backend.model.User;
import com.prym.backend.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    // Test 1: Create session generates a UUID session ID and 7-day expiry
    @Test
    void createSession_Success() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session result = sessionService.createSession(testUser);

        assertNotNull(result.getSessionId());
        assertFalse(result.getSessionId().isEmpty());
        assertEquals(testUser, result.getUser());
        // Expiry should be roughly 7 days from now (at least 6 days to avoid flakiness)
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
        verify(sessionRepository).save(any(Session.class));
    }

    // Test 2: Valid non-expired session returns the linked user
    @Test
    void validateSession_ValidSession() {
        Session session = new Session();
        session.setSessionId("valid-session-id");
        session.setUser(testUser);
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // expires in 7 days

        when(sessionRepository.findBySessionId("valid-session-id")).thenReturn(Optional.of(session));

        Optional<User> result = sessionService.validateSession("valid-session-id");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(sessionRepository, never()).delete(any()); // should not delete a valid session
    }

    // Test 3: Expired session returns empty and deletes itself from the database
    @Test
    void validateSession_ExpiredSession() {
        Session expiredSession = new Session();
        expiredSession.setSessionId("expired-session-id");
        expiredSession.setUser(testUser);
        expiredSession.setExpiresAt(LocalDateTime.now().minusDays(1)); // expired yesterday

        when(sessionRepository.findBySessionId("expired-session-id")).thenReturn(Optional.of(expiredSession));

        Optional<User> result = sessionService.validateSession("expired-session-id");

        assertTrue(result.isEmpty());
        verify(sessionRepository).delete(expiredSession); // expired session must be cleaned up
    }

    // Test 4: Unknown session ID returns empty (no session in DB)
    @Test
    void validateSession_SessionNotFound() {
        when(sessionRepository.findBySessionId("unknown-id")).thenReturn(Optional.empty());

        Optional<User> result = sessionService.validateSession("unknown-id");

        assertTrue(result.isEmpty());
        verify(sessionRepository, never()).delete(any());
    }

    // Test 5: Delete session by session ID delegates to repository
    @Test
    void deleteSession_CallsRepository() {
        sessionService.deleteSession("some-session-id");

        verify(sessionRepository).deleteBySessionId("some-session-id");
    }

    // Test 6: Delete all sessions for a user (logout from all devices)
    @Test
    void deleteAllUserSessions_CallsRepository() {
        sessionService.deleteAllUserSessions(1L);

        verify(sessionRepository).deleteByUserId(1L);
    }
}
