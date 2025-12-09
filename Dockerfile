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
RUN ./gradlew :server:installDist --no-daemon --info

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Add non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create app directory and set permissions
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy distribution with permissions preserved
COPY --from=build --chown=appuser:appgroup /app/server/build/install/server /app

# Ensure the startup script is executable (critical)
RUN chmod +x /app/bin/server

USER appuser

ENV PORT=8080
EXPOSE 8080

CMD ["/app/bin/server"]


