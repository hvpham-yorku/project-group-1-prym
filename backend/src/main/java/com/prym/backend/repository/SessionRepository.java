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

/*
These comments are just to document my understanding of how Spring generates SQL 
 * How Spring generates SQL queries
 
 * Spring auto-generates the SQL, so we only have to write method names
 * 
 * Method name pattern:
 *   findBy + FieldName → SELECT * FROM table WHERE field_name = ?
 *   deleteBy + FieldName → DELETE FROM table WHERE field_name = ?
 * 
 *   findBySessionId(id)         → SELECT * FROM sessions WHERE session_id = ?
 *   findByEmail(email)          → SELECT * FROM users WHERE email = ?
 *   deleteByUserId(id)          → DELETE FROM sessions WHERE user_id = ?
 *   findByRoleAndEmail(r, e)    → SELECT * FROM users WHERE role = ? AND email = ?
 * 
 * Keywords:
 *   And        findByNameAndEmail  WHERE name = ? AND email = ?
 *   Or          findByNameOrEmail   WHERE name = ? OR email = ?
 *   Between     findByAgeBetween    WHERE age BETWEEN ? AND ?
 *   LessThan    findByAgeLessThan   WHERE age < ?
 *   After       findByDateAfter     WHERE date > ?
 *   Before      findByDateBefore    WHERE date < ?
 *   OrderBy     findByNameOrderByAgeDesc  ORDER BY age DESC
 */