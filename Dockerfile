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
FROM eclipse-temurin:17-jre-jammy AS runtime

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory and assign ownership
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy built distribution
COPY --from=build /app/server/build/install/server /app

# Copy Firebase credentials (injected at build time)
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Ensure server script is executable and set ownership
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Set environment variables
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

CMD ["/app/bin/server"]

