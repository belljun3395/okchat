# Multi-stage build for OkChat Spring Boot Application
# Stage 1: Build the application
FROM gradle:8.11-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle configuration files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon --parallel || true

# Copy source code
COPY src ./src

# Build the application with optimizations
RUN gradle bootJar --no-daemon --parallel \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError" \
    && java -Djarmode=layertools -jar build/libs/*.jar extract

# Stage 2: Create the runtime image with layered jars
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Install curl for healthcheck (minimal dependencies)
RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*

# Copy application layers from builder (enables better Docker layer caching)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check with reduced interval for faster detection
HEALTHCHECK --interval=20s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Optimized JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.backgroundpreinitializer.ignore=true"

# Run the application with Spring Boot's layered jar launcher
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher"]