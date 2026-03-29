package com.prym.backend.unit.service;

import com.prym.backend.model.Session;
import com.prym.backend.model.User;
import com.prym.backend.repository.SessionRepository;
import com.prym.backend.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        testUser.setEmail("user@example.com");
        testUser.setRole(User.Role.BUYER);
    }

    // Test 1: createSession_AssignsUuidSessionId
    // A freshly created session must have a non-null, non-blank UUID-format session ID.
    @Test
    void createSession_AssignsUuidSessionId() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session session = sessionService.createSession(testUser);

        assertNotNull(session.getSessionId());
        assertFalse(session.getSessionId().isBlank());
        // UUID pattern: 8-4-4-4-12 hex chars with dashes
        assertTrue(session.getSessionId().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Session ID must be a valid UUID");
    }

    // Test 2: createSession_SetsCorrectUser
    // The session must be linked to the user passed into createSession().
    @Test
    void createSession_SetsCorrectUser() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session session = sessionService.createSession(testUser);

        assertEquals(testUser.getId(), session.getUser().getId());
        assertEquals("user@example.com", session.getUser().getEmail());
    }

    // Test 3: createSession_ExpiresInSevenDays
    // The expiry time must be approximately 7 days in the future (within a 60-second tolerance).
    @Test
    void createSession_ExpiresInSevenDays() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        LocalDateTime beforeCreate = LocalDateTime.now().plusDays(7).minusSeconds(5);
        Session session = sessionService.createSession(testUser);
        LocalDateTime afterCreate = LocalDateTime.now().plusDays(7).plusSeconds(5);

        assertNotNull(session.getExpiresAt());
        assertTrue(session.getExpiresAt().isAfter(beforeCreate),
                "Expiry must be at least 7 days in the future");
        assertTrue(session.getExpiresAt().isBefore(afterCreate),
                "Expiry must not be more than 7 days + 5s in the future");
    }

    // Test 4: createSession_PersistsToRepository
    // createSession() must call sessionRepository.save() exactly once.
    @Test
    void createSession_PersistsToRepository() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        sessionService.createSession(testUser);

        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    // Test 5: createSession_EachCallProducesDifferentSessionId
    // Two consecutive createSession() calls must produce different session IDs (UUIDs are unique).
    @Test
    void createSession_EachCallProducesDifferentSessionId() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session s1 = sessionService.createSession(testUser);
        Session s2 = sessionService.createSession(testUser);

        assertNotEquals(s1.getSessionId(), s2.getSessionId(),
                "Consecutive sessions must have different IDs");
    }

    // Test 6: validateSession_ValidSession_ReturnsUser
    // A session that exists and has not expired must return the associated user.
    @Test
    void validateSession_ValidSession_ReturnsUser() {
        Session activeSession = new Session();
        activeSession.setSessionId("valid-session-id");
        activeSession.setUser(testUser);
        activeSession.setExpiresAt(LocalDateTime.now().plusDays(3)); // still 3 days left

        when(sessionRepository.findBySessionId("valid-session-id"))
                .thenReturn(Optional.of(activeSession));

        Optional<User> result = sessionService.validateSession("valid-session-id");

        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals("user@example.com", result.get().getEmail());
    }

    // Test 7: validateSession_ExpiredSession_ReturnsEmpty
    // An expired session must be deleted and Optional.empty() must be returned.
    @Test
    void validateSession_ExpiredSession_ReturnsEmpty() {
        Session expiredSession = new Session();
        expiredSession.setSessionId("expired-session-id");
        expiredSession.setUser(testUser);
        expiredSession.setExpiresAt(LocalDateTime.now().minusDays(1)); // expired yesterday

        when(sessionRepository.findBySessionId("expired-session-id"))
                .thenReturn(Optional.of(expiredSession));

        Optional<User> result = sessionService.validateSession("expired-session-id");

        assertFalse(result.isPresent(), "Expired session must return empty");
        verify(sessionRepository).delete(expiredSession); // expired session must be cleaned up
    }

    // Test 8: validateSession_NotFound_ReturnsEmpty
    // A session ID that doesn't exist in the repository must return Optional.empty().
    @Test
    void validateSession_NotFound_ReturnsEmpty() {
        when(sessionRepository.findBySessionId("unknown-session-id"))
                .thenReturn(Optional.empty());

        Optional<User> result = sessionService.validateSession("unknown-session-id");

        assertFalse(result.isPresent());
        verify(sessionRepository, never()).delete(any()); // no delete for non-existent sessions
    }

    // Test 9: validateSession_ExpiresAtExactlyNow_TreatedAsExpired
    // A session whose expiresAt is in the past by one second must be treated as expired.
    @Test
    void validateSession_JustExpired_TreatedAsExpired() {
        Session justExpiredSession = new Session();
        justExpiredSession.setSessionId("just-expired-id");
        justExpiredSession.setUser(testUser);
        justExpiredSession.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(sessionRepository.findBySessionId("just-expired-id"))
                .thenReturn(Optional.of(justExpiredSession));

        Optional<User> result = sessionService.validateSession("just-expired-id");

        assertFalse(result.isPresent());
        verify(sessionRepository).delete(justExpiredSession);
    }

    // Test 10: validateSession_ExpiredSession_DoesNotDeleteOtherSessions
    // Deleting one expired session must not affect other sessions in the repository.
    @Test
    void validateSession_ExpiredSession_OnlyDeletesThatSession() {
        Session expiredSession = new Session();
        expiredSession.setSessionId("expired-id");
        expiredSession.setUser(testUser);
        expiredSession.setExpiresAt(LocalDateTime.now().minusDays(2));

        when(sessionRepository.findBySessionId("expired-id"))
                .thenReturn(Optional.of(expiredSession));

        sessionService.validateSession("expired-id");

        // Verify delete is called only with the specific expired session
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).delete(captor.capture());
        assertEquals("expired-id", captor.getValue().getSessionId());
    }

    // Test 11: deleteSession_CallsRepositoryDeleteBySessionId
    // deleteSession() must delegate to sessionRepository.deleteBySessionId() exactly once.
    @Test
    void deleteSession_CallsRepositoryDeleteBySessionId() {
        sessionService.deleteSession("session-to-delete");

        verify(sessionRepository, times(1)).deleteBySessionId("session-to-delete");
    }

    // Test 12: deleteSession_WithNonExistentId_NoException
    // Deleting a session that doesn't exist must not throw — it's a no-op.
    @Test
    void deleteSession_NonExistentId_NoException() {
        doNothing().when(sessionRepository).deleteBySessionId("ghost-session");

        assertDoesNotThrow(() -> sessionService.deleteSession("ghost-session"));
        verify(sessionRepository).deleteBySessionId("ghost-session");
    }

    // Test 13: deleteAllUserSessions_CallsRepositoryDeleteByUserId
    // deleteAllUserSessions() must call sessionRepository.deleteByUserId() with the correct userId.
    @Test
    void deleteAllUserSessions_CallsRepositoryDeleteByUserId() {
        sessionService.deleteAllUserSessions(1L);

        verify(sessionRepository, times(1)).deleteByUserId(1L);
    }

    // Test 14: deleteAllUserSessions_DifferentUser_DeletesOnlyThatUser
    // deleteAllUserSessions() must pass the exact userId to the repository — not some other ID.
    @Test
    void deleteAllUserSessions_DifferentUsers_EachDeletesOnlyOwn() {
        sessionService.deleteAllUserSessions(42L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(sessionRepository).deleteByUserId(captor.capture());
        assertEquals(42L, captor.getValue());
    }
}
