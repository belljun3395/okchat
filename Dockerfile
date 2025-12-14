# Multi-stage build for OkChat Spring Boot Application
# Stage 1: Build the application
FROM gradle:8.11-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle configuration files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY okchat-lib/okchat-lib-web/build.gradle.kts okchat-lib/okchat-lib-web/build.gradle.kts
COPY okchat-lib/okchat-lib-persistence/build.gradle.kts okchat-lib/okchat-lib-persistence/build.gradle.kts
COPY okchat-lib/okchat-lib-ai/build.gradle.kts okchat-lib/okchat-lib-ai/build.gradle.kts

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src
COPY okchat-lib/okchat-lib-web/src okchat-lib/okchat-lib-web/src
COPY okchat-lib/okchat-lib-persistence/src okchat-lib/okchat-lib-persistence/src
COPY okchat-lib/okchat-lib-ai/src okchat-lib/okchat-lib-ai/src

# Build the application
RUN gradle bootJar --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
