package com.prym.backend.repository;

import com.prym.backend.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId); //looks up session when the user makes a request in order to validate it
    void deleteBySessionId(String sessionId);  //This deletes one session when user logs out  
    void deleteByUserId(Long userId); // Deletes all the sessions the user had running which basically means he is logged out from everywhere
    void deleteByExpiresAtBefore(LocalDateTime dateTime);//Cleans up all the sessions that expired before the passed argument 
}