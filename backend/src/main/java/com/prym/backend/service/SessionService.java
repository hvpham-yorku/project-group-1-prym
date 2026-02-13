package com.prym.backend.service;

import com.prym.backend.model.Session; //import the session model class
import com.prym.backend.model.User; // import the user model class
import com.prym.backend.repository.SessionRepository; // import session repository interface
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service // Tells Spring to create one instance of this class and make it available for other classes that need it. It also tells Spring to find other object instances from other classes and fetch them here
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {//here Spring fetches the sessionRepository object from the other @Repository tag in SessionRepository.java
        this.sessionRepository = sessionRepository;
    }

    public Session createSession(User user) { //creates session when the user logs in and saves it to the database 
        Session session = new Session(); // creates new session instance
        session.setSessionId(UUID.randomUUID().toString()); // generates and assigns the session a unique random string ID
        session.setUser(user); //sets the assosiated user with the session created
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // sets expiry time to be in 7 days

        return sessionRepository.save(session); // saves the session instance in database
    }

    public Optional<User> validateSession(String sessionId) { //when user makes a request it checks if the session is valid
        Optional<Session> sessionOptional = sessionRepository.findBySessionId(sessionId);// searches the database for session, returns empty box if not found, box with session if found
       
        if (sessionOptional.isPresent()) {
            Session session = sessionOptional.get();

           
            if (session.getExpiresAt().isAfter(LocalDateTime.now())) { // Check if the session is expired
                return Optional.of(session.getUser());
            } else {
                
                sessionRepository.delete(session); // deletes expired sessions
            }
        }

        return Optional.empty();
    }

    @Transactional //if anything done in the function fails, it keeps track and undos all the actions
    public void deleteSession(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
    }

    @Transactional
    public void deleteAllUserSessions(Long userId) {
        sessionRepository.deleteByUserId(userId);
    }
}