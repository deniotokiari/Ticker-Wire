# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Copy pre-built server distribution
# This should copy bin/, lib/, and other directories from installDist output
COPY --chown=appuser:appgroup server/build/install/server/ /app/

# Copy Firebase credentials
COPY server/serviceAccountKey.json /app/serviceAccountKey.json

# Verify the installation and set permissions
RUN echo "=== Verifying installation ===" && \
    echo "Contents of /app:" && \
    ls -la /app/ && \
    echo "" && \
    echo "=== Checking bin directory ===" && \
    if [ -d /app/bin ]; then \
      ls -la /app/bin/; \
    else \
      echo "❌ ERROR: /app/bin directory not found!"; \
      exit 1; \
    fi && \
    echo "" && \
    echo "=== Checking lib directory ===" && \
    if [ -d /app/lib ]; then \
      echo "✅ Found /app/lib with $(ls -1 /app/lib/*.jar | wc -l) JAR files"; \
      ls -1 /app/lib/*.jar | head -5; \
    else \
      echo "❌ ERROR: /app/lib directory not found!"; \
      echo "Full directory tree:"; \
      find /app -type f -o -type d | head -30; \
      exit 1; \
    fi && \
    echo "" && \
    echo "=== Setting permissions ===" && \
    chmod +x /app/bin/server && \
    chown -R appuser:appgroup /app && \
    echo "✅ Installation verified"

USER appuser

# Set environment variables
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Use the startup script from installDist
# The script automatically sets APP_HOME and constructs CLASSPATH from /app/lib/
CMD ["/app/bin/server"]
