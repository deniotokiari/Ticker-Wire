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
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run the fat JAR
CMD ["java", "-jar", "/app/server.jar"]
