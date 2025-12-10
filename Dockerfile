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

# Copy build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared/ shared/
COPY server/ server/

# Build and fix line endings
RUN dos2unix gradlew \
    && chmod +x gradlew \
    && ./gradlew :server:installDist --no-daemon \
    && dos2unix /build/server/build/install/server/bin/server \
    && chmod +x /build/server/build/install/server/bin/server \
    && echo "=== Build complete ===" \
    && ls -la /build/server/build/install/server/bin/ \
    && ls /build/server/build/install/server/lib/ | grep -i server

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy BOTH bin and lib folders from builder
COPY --from=builder /build/server/build/install/server/bin/ /app/bin/
COPY --from=builder /build/server/build/install/server/lib/ /app/lib/

# Copy service account key
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Verify and set permissions
RUN ls -la /app/ \
    && ls -la /app/bin/ \
    && ls -la /app/lib/ | head -10 \
    && chmod +x /app/bin/server \
    && echo "✅ All files present"

ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Override eclipse-temurin entrypoint and run script through sh explicitly
ENTRYPOINT ["/bin/sh"]
CMD ["/app/bin/server"]
