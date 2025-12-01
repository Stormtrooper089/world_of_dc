# Docker Deployment Guide

This guide explains how to deploy the Cachar District Complaint Management System using Docker with MongoDB Atlas for production.

## Prerequisites

- Docker and Docker Compose installed on your system
- At least 2GB of available RAM
- At least 5GB of available disk space
- MongoDB Atlas account (for production deployment)

## MongoDB Atlas Setup (Production)

### 1. Create MongoDB Atlas Account

1. Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
2. Create a free account or sign in
3. Create a new project

### 2. Create Cluster

1. Click "Create" → "M0 Cluster" (free tier)
2. Choose your cloud provider and region
3. Name your cluster (e.g., "cachar-cluster")
4. Click "Create Cluster"

### 3. Set up Database User

1. Go to "Database Access" → "Add New Database User"
2. Choose "Password" authentication
3. Create username and password
4. Set user privileges to "Read and write to any database"
5. Click "Add User"

### 4. Configure Network Access

1. Go to "Network Access" → "Add IP Address"
2. For development: Add `0.0.0.0/0` (allow all IPs)
3. For production: Add your server's IP address
4. Click "Confirm"

### 5. Get Connection String

1. Go to "Clusters" → "Connect"
2. Choose "Connect your application"
3. Select "Java" and version "4.0 or later"
4. Copy the connection string

### 6. Update Environment Variables

Update your `.env` file with the Atlas connection string:

```env
MONGODB_URI=mongodb+srv://your_username:your_password@your_cluster.mongodb.net/cachar_complaints?retryWrites=true&w=majority
```

## Quick Start

1. **Configure Environment Variables:**
   Update the `.env` file with your MongoDB Atlas credentials:

   ```bash
   # Edit .env file
   MONGODB_URI=mongodb+srv://your_username:your_password@your_cluster.mongodb.net/cachar_complaints?retryWrites=true&w=majority
   JWT_SECRET=your_secure_jwt_secret_here_minimum_256_bits_long_and_random
   SMS_API_KEY=your_actual_sms_api_key
   ```

2. **Validate Configuration:**

   ```bash
   ./validate-deployment.sh
   ```

3. **Deploy Application:**

   ```bash
   docker-compose up --build -d
   ```

4. **Check Status:**

   ```bash
   docker-compose ps
   docker-compose logs -f app
   ```

5. **Access the application:**
   - API: http://localhost:8080
   - Health Check: http://localhost:8080/actuator/health

## Configuration

### Environment Variables

Create a `.env` file in the root directory to override default configurations:

```env
# Database
MONGODB_URI=mongodb://mongodb:27017/cachar_complaints

# Security
JWT_SECRET=your_secure_jwt_secret_here

# SMS Configuration
SMS_API_KEY=your_sms_api_key
SMS_PROVIDER=console  # or your SMS provider

# File Upload
FILE_MAX_SIZE=10485760  # 10MB in bytes
```

### Production Deployment

For production deployment with MongoDB Atlas:

1. Update the `MONGODB_URI` in your `.env` file:

   ```
   MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/cachar_complaints?retryWrites=true&w=majority
   ```

2. Remove or comment out the `mongodb` service in `docker-compose.yml`

3. Deploy only the app service:
   ```bash
   docker-compose up --build -d app
   ```

## File Structure

- `uploads/`: Directory for file uploads (persisted as Docker volume)
- `logs/`: Directory for application logs (persisted as Docker volume)
- `mongodb_data/`: MongoDB data directory (persisted as Docker volume)

## Management Commands

### Stop the application:

```bash
docker-compose down
```

### Rebuild after code changes:

```bash
docker-compose up --build -d
```

### View real-time logs:

```bash
docker-compose logs -f
```

### Scale the application:

```bash
docker-compose up -d --scale app=3
```

### Clean up:

```bash
docker-compose down -v  # Remove containers and volumes
docker system prune     # Clean up unused Docker resources
```

## Troubleshooting

### Check container health:

```bash
docker-compose ps
docker stats
```

### Debug application issues:

```bash
docker-compose logs app
docker-compose exec app bash  # Access container shell
```

### Database connection issues:

- Ensure MongoDB container is running: `docker-compose ps mongodb`
- Check MongoDB logs: `docker-compose logs mongodb`
- Verify connection string in environment variables

### Port conflicts:

- Change the port mapping in `docker-compose.yml`: `ports: - "8081:8080"`

## Security Considerations

1. **Change default JWT secret** in production
2. **Use environment variables** for sensitive configuration
3. **Enable HTTPS** in production (consider using a reverse proxy like Nginx)
4. **Regularly update** base images for security patches
5. **Monitor logs** for security events

## Backup and Restore

### Backup data:

```bash
# Backup uploads
docker run --rm -v world_of_dc_uploads:/data -v $(pwd):/backup alpine tar czf /backup/uploads_backup.tar.gz -C /data .

# Backup database
docker-compose exec mongodb mongodump --db cachar_complaints --out /backup
docker cp $(docker-compose ps -q mongodb):/backup ./mongodb_backup
```

### Restore data:

```bash
# Restore uploads
docker run --rm -v world_of_dc_uploads:/data -v $(pwd):/backup alpine tar xzf /backup/uploads_backup.tar.gz -C /data

# Restore database
docker cp ./mongodb_backup $(docker-compose ps -q mongodb):/backup
docker-compose exec mongodb mongorestore --db cachar_complaints /backup/cachar_complaints
```

## Performance Tuning

### Memory limits:

Adjust JVM memory settings in the Dockerfile:

```dockerfile
ENV JAVA_OPTS="-Xmx1g -Xms512m"
```

### Database optimization:

- Ensure MongoDB has sufficient memory
- Consider using MongoDB Atlas for production
- Enable MongoDB authentication in production

## Monitoring

The application includes Spring Boot Actuator endpoints:

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

Consider integrating with monitoring tools like Prometheus and Grafana for production deployments.
