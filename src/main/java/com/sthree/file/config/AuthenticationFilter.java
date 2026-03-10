package com.sthree.file.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sthree.file.dto.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Authentication filter that extracts user identity from request headers
 *
 * In production, the upstream API gateway / auth-service sets the X-User-Id header
 * after validating the Bearer token. This filter reads that header and sets
 * the "userId" request attribute consumed by controllers via @RequestAttribute
 * 
 * Public endpoints (health checks, shared file downloads) bypass this filter
 */
@Slf4j
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Paths that do not require authentication.
     */
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/files/share/",
            "/actuator/",
            "/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/scalar.html"
    );

    @Value("${services.auth-service-url:http://localhost:8080}")
    private String authServiceUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip auth for public endpoints
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // First: check if gateway already set X-User-Id header 
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader.trim());
                request.setAttribute("userId", userId);
                log.debug("Authenticated user {} from X-User-Id header", userId);
                filterChain.doFilter(request, response);
                return;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-User-Id header value: {}", userIdHeader);
            }
        }

        // Second: check Bearer token in Authorization header
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            UUID userId = validateTokenAndGetUserId(token);

            if (userId != null) {
                request.setAttribute("userId", userId);
                log.debug("Authenticated user {} from Bearer token", userId);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // No valid authentication found
        log.warn("Unauthenticated request to: {} {}", request.getMethod(), path);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.error("Authentication required", "UNAUTHORIZED"));
    }

    /**
     * Validate a Bearer token by calling the auth-service
     * 
     * For now, tries to parse the token as a UUID (useful for dev/testing)
     * 
     * @param token the Bearer token
     * @return the user's UUID, or null if invalid
     */
    private UUID validateTokenAndGetUserId(String token) {
        // Dev mode: accept UUID-format tokens directly
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            // Not a UUID-format token; in production, call auth-service here
            log.debug("Token is not UUID format, auth-service validation not yet implemented");
            return null;
        }
    }

    /**
     * Check if a request path is public (no auth required)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
