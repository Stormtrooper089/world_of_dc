package org.dcoffice.cachar.config;

import org.dcoffice.cachar.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Security Configuration for Cachar Complaint Management System
 *
 * This configuration defines:
 * - Public endpoints for citizens (OTP, complaint creation, tracking)
 * - Protected endpoints for officers (assignment, status updates)
 * - Role-based access control
 * - Password encoding
 * - Security headers
 *
 * @author District Cachar IT Team
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtService jwtService;

    /**
     * Configure HTTP Security
     *
     * @param http HttpSecurity configuration
     * @throws Exception if configuration fails
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for API endpoints (using tokens instead)
                .csrf().disable()

                // Configure CORS
                .cors().and()

                // Configure session management (stateless for API)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // Configure authorization rules
                .authorizeRequests()

                // ===== PUBLIC ENDPOINTS (No Authentication Required) =====

                // Citizen registration and OTP endpoints
                .antMatchers("/api/citizen/send-otp").permitAll()
                .antMatchers("/api/citizen/verify-otp").permitAll()
                .antMatchers("/api/citizen/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/citizen/profile/**").permitAll()

                // Public complaint endpoints (citizens can track their complaints)
                .antMatchers(HttpMethod.GET, "/api/complaints/track/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/complaints/citizen/**").permitAll()

                // Public file download (for citizens to view their attachments)
                .antMatchers(HttpMethod.GET, "/api/files/download/**").permitAll()

                // Health check and monitoring (public for load balancers)
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/actuator/info").permitAll()

                // H2 Console (Development only - disable in production)
                .antMatchers("/h2-console/**").permitAll()

                // Static resources
                .antMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                // ===== PUBLIC OFFICER ENDPOINTS (No Authentication Required) =====
                
                // Officer signup and login (public)
                .antMatchers(HttpMethod.POST, "/api/officer/signup").permitAll()
                .antMatchers(HttpMethod.POST, "/api/officer/login").permitAll()

                // ===== PROTECTED ENDPOINTS (Authentication Required) =====

                // Other officer management endpoints (require authentication)
                .antMatchers("/api/officer/**").authenticated()

                // Administrative complaint endpoints (require officer authentication)
                .antMatchers(HttpMethod.POST, "/api/complaints/create").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/unassigned").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/recent/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/category/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/status/**").authenticated()

                // File management for officers
                .antMatchers("/api/files/complaint/**").authenticated()

                // Administrative monitoring endpoints
                .antMatchers("/actuator/metrics").authenticated()
                .antMatchers("/actuator/**").hasRole("ADMIN")

                // ===== ROLE-BASED ACCESS (Defined in controllers with @PreAuthorize) =====
                // - Complaint assignment: DISTRICT_COMMISSIONER, ADDITIONAL_DISTRICT_COMMISSIONER
                // - Status updates: Any authenticated officer
                // - Report generation: DISTRICT_COMMISSIONER, ADDITIONAL_DISTRICT_COMMISSIONER

                // All other requests require authentication
                .anyRequest().authenticated()

                .and()

                // Configure security headers
                .headers(headers -> headers
                        .frameOptions().deny() // Prevent clickjacking (disable for H2 console in dev)
                        .contentTypeOptions().and()
                        .xssProtection().and()
                        .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
//                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
//                                .maxAgeInSeconds(31536000)
//                                .includeSubdomains(true)
//                        )
                )

                // Add JWT authentication filter
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // Configure HTTP Basic Authentication (replace with JWT in production)
                .httpBasic()

                // Configure exception handling
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Authentication required\",\"timestamp\":\"" +
                                    java.time.LocalDateTime.now() + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Access denied. Insufficient privileges\",\"timestamp\":\"" +
                                    java.time.LocalDateTime.now() + "\"}"
                    );
                });

        // Special configuration for H2 console (Development only)
        if (isDevelopmentProfile()) {
            http.headers().frameOptions().sameOrigin();
        }
    }

    /**
     * Password encoder bean for BCrypt hashing
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt with strength 12 for better security
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Check if running in development profile
     *
     * @return true if development profile is active
     */
    private boolean isDevelopmentProfile() {
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        return activeProfiles.contains("dev") || activeProfiles.contains("local");
    }

    /**
     * Create JWT authentication filter bean
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }
}

/**
 * Additional Security Notes:
 *
 * 1. PRODUCTION SECURITY CHECKLIST:
 *    - Replace HTTP Basic Auth with JWT tokens
 *    - Enable HTTPS only
 *    - Configure proper CORS origins
 *    - Disable H2 console
 *    - Enable security headers
 *    - Set up proper SSL certificates
 *    - Configure rate limiting
 *    - Set up monitoring and alerting
 *
 * 2. AUTHENTICATION FLOW:
 *    - Citizens: No authentication required for basic operations
 *    - Officers: HTTP Basic Auth with employee ID and password
 *    - Future: Implement OAuth2/JWT for better security
 *
 * 3. AUTHORIZATION LEVELS:
 *    - Public: Citizen registration, OTP, complaint creation, tracking
 *    - Officer: View assigned complaints, update status
 *    - DC/ADC: All officer permissions + complaint assignment
 *    - Admin: System monitoring and configuration
 *
 * 4. ROLE HIERARCHY (Spring Security):
 *    ROLE_ADMIN > ROLE_DISTRICT_COMMISSIONER > ROLE_OFFICER > ROLE_CITIZEN
 */