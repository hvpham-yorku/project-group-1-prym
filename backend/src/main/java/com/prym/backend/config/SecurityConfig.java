package com.prym.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration //tells Spring that this class has configuration settings
@EnableWebSecurity//turns on web security features
public class SecurityConfig {

    private final SessionFilter sessionFilter;

    public SecurityConfig(SessionFilter sessionFilter) {
        this.sessionFilter = sessionFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // enable cross origin resource sharing which allows frontend to talk to backend
            .csrf(csrf -> csrf.disable())//disable CSRF protection since it is not needed
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // prevents Spring from creating its own sessions since we are managing them ourselves with the Session table
            .authorizeHttpRequests(auth -> auth //defines who can access what urls
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/buyer/**").hasRole("BUYER") // only buyers can access
                .requestMatchers("/api/seller/**").hasAuthority("ROLE_SELLER") //only Sellers can access
                .anyRequest().authenticated() //everything else requires just being logged in
            )
            .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class); // sets the priority of our SessionFilter to run first before Spring's default authentication filter. This is where we check the SESSION_ID cookie

        return http.build(); // builds and returns the security configuration
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));//only allow requests from the react frontend
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));// Allow these http methods
        configuration.setAllowedHeaders(Arrays.asList("*"));//allow any headers
        configuration.setAllowCredentials(true);//allow cookies to be sent with requests

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);//apply this configuration to all urls
        return source;
    }
}