# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the pre-built jar file
COPY build/libs/*.jar app.jar

# Set the startup command
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Expose the default Spring Boot port
EXPOSE 8080
