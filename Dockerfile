# Build stage
FROM gradle:9.2.1-jdk17 AS build
WORKDIR /app

# Copy Gradle configuration files (including version catalog)
COPY settings.gradle ./
COPY gradle.properties ./
COPY gradle ./gradle

# Copy source code (app module contains its own build.gradle)
COPY app ./app

# Build the application using gradle command directly (image already has Gradle)
RUN gradle :app:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/app/build/libs/app.jar app.jar

# Expose port
EXPOSE 3000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

