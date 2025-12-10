# Dockerfile for TickerWire Server

FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy AS builder

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

# Build FAT JAR instead of distribution
RUN dos2unix gradlew && chmod +x gradlew \
    && ./gradlew :server:buildFatJar --no-daemon \
    && echo "=== FAT JAR location ===" \
    && find /build -name "*.jar" -path "*libs*" | head -5

# Runtime
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the fat jar (use exact filename, not wildcard)
COPY --from=builder /build/server/build/libs/server-all.jar /app/server.jar

# Copy service account key
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Verify and set permissions (Cloud Run runs as non-root)
RUN ls -la /app/ && test -f /app/server.jar && echo "✅ server.jar exists" \
    && chmod 644 /app/server.jar \
    && chmod 644 /app/serviceAccountKey.json \
    && chmod 755 /app

ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run the fat jar directly
ENTRYPOINT ["java"]
CMD ["-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/server.jar"]
