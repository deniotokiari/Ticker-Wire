# Multi-stage Dockerfile for TickerWire Server
# Optimized for Google Cloud Run deployment

# Stage 1: Build
FROM gradle:8-jdk17 AS build

WORKDIR /app

# Copy gradle files first for better caching
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle.properties .

# Copy version catalog
COPY gradle/libs.versions.toml gradle/

# Copy project modules
COPY shared shared
COPY server server

# Build the server distribution
RUN gradle :server:installDist --no-daemon --info

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built distribution from build stage
COPY --from=build /app/server/build/install/server .

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Cloud Run uses PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/health || exit 1

# Run the server
CMD ["./bin/server"]

