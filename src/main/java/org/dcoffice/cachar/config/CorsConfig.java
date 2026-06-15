package org.dcoffice.cachar.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    private static final String[] ALLOWED_ORIGINS = {
            "https://world-of-dc-ui-ten.vercel.app",
            "https://world-of-dc-ui.vercel.app",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080"
    };

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(ALLOWED_ORIGINS)
                        .allowedOriginPatterns("https://world-of-dc-ui-*.vercel.app")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Content-Disposition")
                        .maxAge(3600);
            }
        };
    }
}
