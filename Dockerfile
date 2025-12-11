# Dockerfile for TickerWire Server
# Uses pre-built fat JAR (built in CI)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Copy pre-built fat JAR
COPY --chown=appuser:appgroup server/build/libs/server-*.jar /app/server.jar

# Copy Firebase credentials
COPY --chown=appuser:appgroup server/serviceAccountKey.json /app/serviceAccountKey.json

# Verify the JAR file exists
RUN echo "=== Verifying installation ===" && \
    if [ ! -f /app/server.jar ]; then \
      echo "❌ ERROR: server.jar not found!"; \
      exit 1; \
    fi && \
    echo "✅ Found server.jar: $(ls -lh /app/server.jar)" && \
    echo "✅ Installation verified"

USER appuser

# Set environment variables
# Note: PORT is set by Cloud Run dynamically, don't override it
# FIREBASE_CONFIG_PATH is optional - Cloud Run can use Application Default Credentials
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run the fat JAR with optimized JVM settings for Cloud Run
# -XX:+UseContainerSupport: Use container-aware memory settings
# -XX:MaxRAMPercentage=75.0: Use 75% of available memory (Cloud Run sets memory limits)
# -XX:+ExitOnOutOfMemoryError: Exit immediately on OOM (Cloud Run will restart)
# -Djava.security.egd=file:/dev/./urandom: Faster startup (non-blocking entropy)
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/server.jar"]
