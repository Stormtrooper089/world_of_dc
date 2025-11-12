package org.dcoffice.cachar.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.dcoffice.cachar.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
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

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                String officerId = claims.getSubject();
                String role = claims.get("role", String.class);
                
                logger.debug("Parsed claims - officerId: {}, role: {}", officerId, role);
                
                if (officerId != null && role != null) {
                    // Create authorities based on role
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                    );
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(officerId, null, authorities);
                    
                    // Set the authentication in the security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.info("JWT authentication successful for officer: {} with role: {}", officerId, role);
                } else {
                    logger.warn("JWT token missing required claims - officerId: {}, role: {}", officerId, role);
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
}
