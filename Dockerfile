##############################
# 1) BUILD STAGE (Maven)
##############################
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy dependencies first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy full source
COPY src ./src

# Build the JAR (skip tests for speed)
RUN mvn clean package -DskipTests



##############################
# 2) RUNTIME STAGE (Alpine JDK)
##############################
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Create runtime directories
RUN mkdir -p /app/logs /app/uploads

# Copy built jar (auto-detect JAR)
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

##############################
# LOGGING IMPROVEMENTS
##############################

# JVM logging options
ENV JAVA_OPTS="\
  -Xms256m \
  -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75 \
  -verbose:gc \
  -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags \
"

# Spring Boot logging to file
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
ENV LOGGING_FILE_PATH=/app/logs

##############################
# HEALTHCHECK WITHOUT CURL
##############################
# Use wget (small on Alpine)
RUN apk add --no-cache wget

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1


##############################
# START THE APPLICATION
##############################
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
