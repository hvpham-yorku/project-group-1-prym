package com.prym.backend.config;

import com.prym.backend.model.User;
import com.prym.backend.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component //tells Spring to create an instance of this file and manage it
public class SessionFilter extends OncePerRequestFilter { //a OncePerRequestFilter is a filter that runs once for every http request

    private final SessionService sessionService; // we need this to validate the sessions

    public SessionFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) // this method runs on every request to our API
            throws ServletException, IOException {

        // Try to get SESSION_ID cookie from the request
        String sessionId = getSessionIdFromCookies(request);

        //If we have a session ID, validate it
        if (sessionId != null) {
            Optional<User> userOptional = sessionService.validateSession(sessionId);

            // If our session is valid, tell Spring Security this user is authenticated
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))); //arguments are email, password, and what roles they have

                SecurityContextHolder.getContext().setAuthentication(authToken); //tells spring that the request is from an authenticated user
            }
        }

        //  Continue to the next filter / actual endpoint
        filterChain.doFilter(request, response);
    }

    private String getSessionIdFromCookies(HttpServletRequest request) { //helper method to find SESSION_ID cookie
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) { //loops through cookies
                if ("SESSION_ID".equals(cookie.getName())) {
                    return cookie.getValue(); //if found return its value
                }
            }
        }

        return null; // return null if not found
    }
}
/*
this is how the session filter works in detail 
 * This filter runs on every request to check if the user is logged in.
 * 
 * session flow if its valid:
 * 1. User sends a request with a SESSION_ID cookie
 * 2. Filter gets the session ID from the cookie
 * 3. Filter looks up session ID in the database
 * 4. if the session is found and it is not expired then we get the user
 * 5. Tell Spring Security that this user is authenticated with X role
 * 6. SecurityConfig checks if user's role can access the URL
 * 7. if the role matches then the request goes through
 * 
 * session flow if its invalid:
 * 1. User sends request with no cookie or bad session ID
 * 2. Filter can't find a valid session in database
 * 3. Filter does nothing (user stays as guest)
 * 4. SecurityConfig checks if URL requires authentication
 * 5. URL requires login but the user is a guest so we throw a 401 error message unauthorized user
 * 
 */