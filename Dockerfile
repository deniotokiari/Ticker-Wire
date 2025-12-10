# Dockerfile for TickerWire Server
# Multi-stage build for reliability

# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copy Gradle wrapper and build files first (for layer caching)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy project source files
COPY shared/ shared/
COPY server/ server/

# Make gradlew executable and build
RUN chmod +x gradlew && ./gradlew :server:installDist --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy built distribution from builder stage
COPY --from=builder /build/server/build/install/server/ /app/

# Copy Firebase service account credentials (injected at build time)
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Set permissions
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Cloud Run uses PORT environment variable
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run the server
CMD ["/app/bin/server"]
