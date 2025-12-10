# Dockerfile for TickerWire Server
# Multi-stage build for reliability

# Stage 1: Build
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy AS builder

# Install Android SDK command-line tools (required for shared module)
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

RUN apt-get update && apt-get install -y --no-install-recommends \
    unzip \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android command-line tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
    sdkmanager "platforms;android-36" "build-tools;36.0.0" > /dev/null

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

# Stage 2: Runtime (slim image without Android SDK)
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy

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
