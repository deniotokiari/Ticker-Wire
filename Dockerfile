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

# Build only the JVM target of shared module and then the server
# This avoids needing Android SDK which isn't in this Docker image
RUN ./gradlew :shared:jvmJar :server:installDist --no-daemon \
    && echo "=== Build completed ===" \
    && ls -la /app/server/build/install/server/ \
    && ls -la /app/server/build/install/server/bin/ \
    && test -f /app/server/build/install/server/bin/server || (echo "ERROR: server script not found!" && exit 1)

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy AS runtime

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory and assign ownership
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy built distribution
COPY --from=build /app/server/build/install/server/ /app/

# Copy Firebase credentials (injected at build time)
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Verify files were copied and set permissions
RUN ls -la /app/ && \
    ls -la /app/bin/ && \
    test -f /app/bin/server || (echo "ERROR: /app/bin/server not found after copy!" && exit 1) && \
    chmod +x /app/bin/server && \
    chown -R appuser:appgroup /app

USER appuser

# Set environment variables
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

CMD ["/app/bin/server"]
