package org.dcoffice.cachar.config;

import io.jsonwebtoken.Claims;
import org.dcoffice.cachar.repository.TrackingMemberRepository;
import org.dcoffice.cachar.service.CitizenService;
import org.dcoffice.cachar.service.JwtService;
import org.dcoffice.cachar.service.OfficerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CitizenService citizenService;
    private final OfficerService officerService;
    private final TrackingMemberRepository trackingMemberRepository;

    public JwtAuthenticationFilter(JwtService jwtService, CitizenService citizenService,
                                   OfficerService officerService, TrackingMemberRepository trackingMemberRepository) {
        this.jwtService = jwtService;
        this.citizenService = citizenService;
        this.officerService = officerService;
        this.trackingMemberRepository = trackingMemberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        logger.debug("Processing request: {} with auth header: {}", request.getRequestURI(), authHeader);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.debug("Extracted token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            
            try {
                Claims claims = jwtService.parseToken(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                logger.debug("Parsed claims - userId: {}, role: {}", userId, role);

                if (userId != null && role != null) {
                    // Validate user exists and get user object
                    Object userDetails = validateAndGetUser(userId, role);
                    if (userDetails == null) {
                        logger.warn("User authentication failed - user not found: {} with role: {}", userId, role);
                        return; // Don't set authentication
                    }

                    // Create authorities based on role
                    String authority = "ROLE_" + role;
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(authority)
                    );

                    // Create authentication with user object in details
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(userDetails);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("JWT authentication successful for {}: {} with authority: {}", role.toLowerCase(), userId, authority);
                } else {
                    logger.warn("JWT token missing required claims - userId: {}, role: {}", userId, role);
                }
            } catch (Exception e) {
                logger.error("JWT authentication failed: {}", e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }
        } else {
            logger.debug("No valid Authorization header found");
        }
        
        filterChain.doFilter(request, response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean isPublicCitizenEndpoint =
                path.equals("/api/citizen/send-otp") ||
                path.equals("/api/citizen/login") ||
                path.equals("/api/citizen/verify-otp") ||
                path.equals("/api/citizen/register") ||
                path.equals("/api/citizen/carousel") ||
                path.equals("/api/citizen/portal-stats") ||
                ("GET".equalsIgnoreCase(method) && path.startsWith("/api/citizen/profile/"));

        // ✅ Skip JWT filter for public endpoints
        return path.startsWith("/api/vehicles") ||
                path.startsWith("/api/polling-parties") ||
                path.startsWith("/api/tracking") ||
                path.startsWith("/api/waste-pickup") ||
                path.startsWith("/auth") ||
                isPublicCitizenEndpoint ||
                path.startsWith("/actuator") ||
                path.startsWith("/h2-console");
    }
    /**
     * Common user validation method for both citizens and officers
     */
    private Object validateAndGetUser(String userId, String role) {
        try {
            switch (role) {
                case "CITIZEN":
                    return citizenService.findById(userId).orElse(null);
                case "WORKER":
                case "WORKER_ADMIN":
                    return trackingMemberRepository.findById(userId).orElse(null);
                case "ADMIN":
                case "SMC_COMMISSIONER":
                case "OFFICER":
                case "DISTRICT_COMMISSIONER":
                case "ADDITIONAL_DISTRICT_COMMISSIONER":
                case "ASSISTANT_DISTRICT_COMMISSIONER":
                case "ASSISTANT_COMMISSIONER":
                case "CIRCLE_OFFICER":
                case "BLOCK_DEVELOPMENT_OFFICER":
                case "GRAM_PANCHAYAT_OFFICER":
                case "TEHSILDAR":
                case "SUB_DIVISIONAL_OFFICER":
                case "HEALTH_OFFICER":
                case "EDUCATION_OFFICER":
                case "REVENUE_OFFICER":
                case "AGRICULTURE_OFFICER":
                case "PUBLIC_WORKS_OFFICER":
                case "PWD_OFFICER":
                case "POLICE_OFFICER":
                    return officerService.findById(userId).orElse(null);
                default:
                    logger.warn("Unknown role for authentication: {}", role);
                    return null;
            }
        } catch (Exception e) {
            logger.error("Error validating user {} with role {}: {}", userId, role, e.getMessage());
            return null;
        }
    }
}
