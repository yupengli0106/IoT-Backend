# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Run
- **Build**: `./mvnw clean package` (or `mvn clean package` if Maven is installed globally)
- **Run**: `./mvnw spring-boot:run` (or `java -jar target/MyApp-0.0.1-SNAPSHOT.jar`)
- **Test**: `./mvnw test` (runs all unit tests)
- **Run single test**: `./mvnw test -Dtest=ClassName#methodName`

### Docker Commands
- **Build image**: `docker build -t device-control-system .`
- **Run container**: `docker run -d --name device-control-app -p 8080:8080 device-control-system`
- **Run with Docker Compose**: `docker-compose up -d` (requires `.env` file with environment variables)

### Device Simulator
- **Run Python simulator**: `cd Simulator && python sensor158.py` (requires Python 3.9+ and dependencies)

## Code Architecture

### Core Components
- **Main Application**: `MyAppApplication.java` - Spring Boot entry point with caching and scheduling enabled
- **Security**: JWT-based authentication with Spring Security (`SecurityConfig.java`, `JwtRequestFilter.java`)
- **Device Management**: Full CRUD with MQTT integration for real-time control
- **Real-time Communication**: WebSocket handler for live dashboard updates
- **Caching**: Redis-based caching for device stats and pagination (first 3 pages only)

### Key Services
- **DeviceService**: Core device CRUD operations with MQTT publishing/subscribing
- **MqttService**: Handles MQTT broker communication, message routing, and device data processing
- **UserActivityService**: Logs all user actions with timestamps
- **CachedDeviceService**: Manages Redis cache invalidation for device-related data

### Data Flow
1. **Device Control**: REST API → DeviceService → MQTT publish → Physical device
2. **Device Data**: Physical device → MQTT → MqttService → WebSocket → Frontend dashboard  
3. **Energy Data**: MQTT → Database storage for historical charts
4. **User Activities**: All operations logged with timestamps for audit trail

### Database Layer
- **MyBatis**: XML-based SQL mapping in `src/main/resources/mapper/`
- **MySQL**: Primary database with connection pooling
- **Redis**: Caching layer for performance optimization

### Configuration
- **Environment Variables**: Database, Redis, MQTT, and email credentials
- **Application Config**: `application.yml` with external property placeholders
- **Docker**: Multi-service setup with MySQL, Redis, and Mosquitto MQTT broker

### Testing Strategy
- Unit tests in `src/test/java/` following Spring Boot testing patterns
- Mapper tests for database layer validation
- JWT utility testing for security components

### MQTT Integration
- **Topics**: `home/device/{deviceId}/status` (control), `home/device/{deviceId}/data` (sensor data)
- **QoS Level**: 1 (at least once delivery)
- **Reconnection**: Automatic resubscription to stored topics on service restart
- **Data Types**: Sensor data (real-time display) vs energy data (database storage)

### Frontend Integration
- Separate React frontend repository at: https://github.com/yupengli0106/myapp-react.git
- WebSocket connection for real-time dashboard updates
- RESTful API communication for CRUD operations