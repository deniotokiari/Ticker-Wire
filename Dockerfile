# Dockerfile for TickerWire Server

FROM eclipse-temurin:17-jdk-jammy AS builder

# Install Android SDK (required for shared module)
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin"

RUN apt-get update && apt-get install -y --no-install-recommends unzip wget \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses > /dev/null 2>&1 \
    && sdkmanager "platforms;android-36" "build-tools;36.0.0" > /dev/null

WORKDIR /build

# Copy build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared/ shared/
COPY server/ server/

# Build
RUN chmod +x gradlew \
    && ./gradlew :server:installDist --no-daemon \
    && echo "=== Build complete ===" \
    && echo "=== Full install directory ===" \
    && ls -laR /build/server/build/install/server/ \
    && echo "=== Looking for server JAR ===" \
    && ls /build/server/build/install/server/lib/ | grep -i server

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy JARs from builder
COPY --from=builder /build/server/build/install/server/lib/ /app/lib/

# Copy service account key directly (created by CI before docker build)
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Verify
RUN ls -la /app/ && ls -la /app/lib/ | head -10 && test -f /app/serviceAccountKey.json && echo "✅ All files present"

ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run Java directly
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-cp", "/app/lib/*", "pl.deniotokiari.tickerwire.ApplicationKt"]
