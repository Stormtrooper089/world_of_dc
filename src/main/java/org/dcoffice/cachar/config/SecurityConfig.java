package org.dcoffice.cachar.config;

import org.dcoffice.cachar.service.CitizenService;
import org.dcoffice.cachar.service.JwtService;
import org.dcoffice.cachar.service.OfficerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy
    private CitizenService citizenService;

    @Autowired
    @Lazy
    private OfficerService officerService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()

                // ===== PUBLIC ENDPOINTS =====
                .antMatchers("/api/citizen/send-otp").permitAll()
                .antMatchers("/api/citizen/verify-otp").permitAll()
                .antMatchers("/api/citizen/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/citizen/profile/{mobileNumber}").permitAll()
                .antMatchers(HttpMethod.GET, "/api/citizen/carousel").permitAll()
                .antMatchers(HttpMethod.GET, "/api/citizen/portal-stats").permitAll()
                .antMatchers(HttpMethod.GET, "/api/complaints/track/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/files/download/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/actuator/info").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .antMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .antMatchers(HttpMethod.POST, "/api/officer/signup").permitAll()
                .antMatchers(HttpMethod.POST, "/api/officer/login").permitAll()

                // ✅ Polling station endpoints (public for dashboard ingestion)// ✅ Polling station endpoints (public for dashboard ingestion)
                .antMatchers("/api/polling-stations/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/polling-stations/routes/pdf").permitAll()

                // ===== PROTECTED ENDPOINTS =====
                .antMatchers("/api/officer/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/unassigned").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/recent/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/category/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/complaints/status/**").authenticated()
                .antMatchers("/api/files/complaint/**").authenticated()
                .antMatchers("/actuator/metrics").authenticated()
                .antMatchers("/actuator/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
                .and()
                .headers(headers -> headers
                        .frameOptions().deny()
                        .contentTypeOptions().and()
                        .xssProtection().and()
                        .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .httpBasic()
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

        if (isDevelopmentProfile()) {
            http.headers().frameOptions().sameOrigin();
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private boolean isDevelopmentProfile() {
        String activeProfiles = System.getProperty("spring.profiles.active", "");
        return activeProfiles.contains("dev") || activeProfiles.contains("local");
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, citizenService, officerService);
    }
}