# Stage 1: Build the application
FROM eclipse-temurin:22-jdk AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
COPY .mvn/ .mvn
COPY mvnw .
COPY mvnw.cmd .

# Make mvnw executable
RUN chmod +x mvnw

# Package the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:22-jre

# Set the working directory
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar /app/app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the jar file
CMD ["java", "-jar", "/app/app.jar"]
