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

# Build and verify output exists
RUN chmod +x gradlew \
    && ./gradlew :server:installDist --no-daemon \
    && echo "=== Build complete, verifying output ===" \
    && ls -la /build/server/build/install/server/ \
    && ls -la /build/server/build/install/server/bin/ \
    && test -f /build/server/build/install/server/bin/server \
    && echo "✅ Server binary created successfully"

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /app && chown -R app:app /app

WORKDIR /app

COPY --from=builder /build/server/build/install/server/ /app/
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Verify copy worked and set permissions
RUN ls -la /app/ \
    && ls -la /app/bin/ \
    && test -f /app/bin/server || (echo "❌ COPY FAILED: /app/bin/server not found" && exit 1) \
    && chmod +x /app/bin/server \
    && chown -R app:app /app \
    && echo "✅ Runtime image ready"

USER app

ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

CMD ["/app/bin/server"]
