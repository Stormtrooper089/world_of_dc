# Use Eclipse Temurin OpenJDK 17 as the base image
FROM eclipse-temurin:17-jdk

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Create directories for file uploads and logs
RUN mkdir -p uploads logs

# Copy the JAR file from target directory
COPY target/world_of_dc-1.0-SNAPSHOT.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables for production (can be overridden)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
