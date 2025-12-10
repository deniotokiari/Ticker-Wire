# Dockerfile for TickerWire Server

FROM eclipse-temurin:17-jdk-jammy AS builder

# Install Android SDK (required for shared module)
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin"

RUN apt-get update && apt-get install -y --no-install-recommends unzip wget dos2unix \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses > /dev/null 2>&1 \
    && sdkmanager "platforms;android-36" "build-tools;36.0.0" > /dev/null

WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared/ shared/
COPY server/ server/

# Build
RUN dos2unix gradlew && chmod +x gradlew \
    && ./gradlew :server:installDist --no-daemon \
    && dos2unix /build/server/build/install/server/bin/server \
    && chmod +x /build/server/build/install/server/bin/server

# Runtime - use simpler base
FROM debian:bookworm-slim

# Install JRE
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy entire server distribution directory
COPY --from=builder /build/server/build/install/server/ /app/

# Copy service account key
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Debug: show what was copied
RUN echo "=== /app contents ===" && ls -laR /app/ && echo "=== End ===" \
    && test -f /app/bin/server && echo "✅ server script exists" \
    && chmod +x /app/bin/server

ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

EXPOSE 8080

CMD ["/app/bin/server"]
