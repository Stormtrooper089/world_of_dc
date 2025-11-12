Windows setup and run instructions for the Cachar complaint system

Prerequisites
- Java 17 JDK (Adoptium/Eclipse Temurin, Oracle, Amazon Corretto, etc.)
- Apache Maven (3.6+)
- MongoDB (local or Atlas)
- PowerShell (Windows default)

1) Install Java 17
- Download and install a JDK 17 distribution. Recommended: Eclipse Temurin (https://adoptium.net/).
- After install, set JAVA_HOME to the JDK install folder and add %JAVA_HOME%\bin to PATH.

Verify:
```powershell
mvn -v
java -version
```

2) Install Maven
- Download Maven (https://maven.apache.org/download.cgi) and unpack it.
- Set M2_HOME to the Maven folder and add %M2_HOME%\bin to PATH.

3) Install and run MongoDB (local)
- Option A: Install MongoDB Community Server from https://www.mongodb.com/try/download/community and run the service.
- Option B: Run MongoDB using Docker (requires Docker Desktop):
```powershell
# Start a MongoDB container
# (maps host 27017 to container 27017 and creates a named volume for persistence)
docker run -d --name world-of-dc-mongo -p 27017:27017 -v world_of_dc_mongo_data:/data/db mongo:6.0
```

Default DB config used by the project (development):
- mongodb://localhost:27017/cachar_complaints

4) Build and run the project
- From project root:
```powershell
# Build
mvn clean package

# Run with dev profile
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run

# Alternatively run the jar built in target/
java -jar .\target\world_of_dc-1.0-SNAPSHOT.jar
```

5) File uploads & logs
- The app creates `./uploads` for uploaded files and `logs/` for the log file. Ensure the user running the app has write access.

6) Optional environment variables
- For production MongoDB (example):
```powershell
$env:MONGODB_URI = 'mongodb://username:password@host:27017/cachar_complaints'
$env:SPRING_PROFILES_ACTIVE = 'prod'
```

7) Health check
- Once running, check: http://localhost:8080/actuator/health

Troubleshooting
- If startup fails with MongoDB connection errors, ensure MongoDB is running and reachable at the configured URI.
- If build fails due to Java version, install JDK 17 and ensure `java -version` shows Java 17.

If you'd like, I can add a PowerShell script to automate environment setup and starting the app.
