package org.dcoffice.cachar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MongoConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    private final Environment environment;
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    public MongoConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        // Log all MongoDB-related environment variables
        logger.info("========================================");
        logger.info("MongoDB Connection Configuration:");
        logger.info("Environment Variables:");
        logger.info("  MONGODB_URI: {}", environment.getProperty("MONGODB_URI", "NOT SET"));
        logger.info("  SPRING_DATA_MONGODB_URI: {}", environment.getProperty("SPRING_DATA_MONGODB_URI", "NOT SET"));
        logger.info("Spring Properties:");
        logger.info("  spring.data.mongodb.uri: {}", mongoUri != null ? maskMongoUri(mongoUri) : "NULL");
        logger.info("========================================");
    }

    private String maskMongoUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "NOT SET";
        }
        
        // Mask password in connection string
        // Format: mongodb+srv://username:password@host/database
        if (uri.contains("@")) {
            try {
                int atIndex = uri.indexOf("@");
                int protocolEnd = uri.indexOf("://") + 3;
                if (protocolEnd > 2 && atIndex > protocolEnd) {
                    String beforeAt = uri.substring(0, atIndex);
                    String afterAt = uri.substring(atIndex);
                    
                    // Check if there's a colon (username:password)
                    int colonIndex = beforeAt.indexOf(":", protocolEnd);
                    if (colonIndex > 0) {
                        String username = beforeAt.substring(protocolEnd, colonIndex);
                        String masked = beforeAt.substring(0, protocolEnd) + username + ":****@";
                        return masked + afterAt.substring(1);
                    }
                }
            } catch (Exception e) {
                // If masking fails, return a generic message
                return "mongodb://****:****@****";
            }
        }
        
        return uri;
    }
}
