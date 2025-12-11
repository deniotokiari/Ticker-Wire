# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy pre-built server distribution (built in CI)
# Copy entire directory structure - this will fail if source doesn't exist
COPY --chown=appuser:appgroup server/build/install/server/ /app/

# Debug: Verify what was copied (this will help diagnose issues)
RUN echo "=== Debugging: Contents of /app after COPY ===" && \
    echo "Full directory listing:" && \
    ls -la /app/ && \
    echo "" && \
    echo "Checking for /app/bin:" && \
    (test -d /app/bin && echo "✅ /app/bin exists" || echo "❌ /app/bin does NOT exist") && \
    echo "" && \
    echo "Checking for /app/bin/server:" && \
    (test -f /app/bin/server && echo "✅ /app/bin/server exists" || echo "❌ /app/bin/server does NOT exist") && \
    echo "" && \
    echo "Contents of /app/bin (if exists):" && \
    (ls -la /app/bin/ || echo "Cannot list /app/bin") && \
    echo "" && \
    echo "Contents of /app/lib (first 5 files, if exists):" && \
    (ls -la /app/lib/ | head -5 || echo "Cannot list /app/lib")

# Copy Firebase credentials
# For local builds: use server/serviceAccountKey.json
# For CI builds: file is created at server/serviceAccountKey.json before Docker build
# Note: Cloud Run can also use Application Default Credentials if this file is not present
COPY server/serviceAccountKey.json /app/serviceAccountKey.json

# Verify server binary exists and set permissions
RUN if [ ! -f /app/bin/server ]; then \
      echo "ERROR: /app/bin/server not found!"; \
      echo "Contents of /app:"; \
      ls -la /app/; \
      echo "Contents of /app/bin (if exists):"; \
      ls -la /app/bin/ 2>/dev/null || echo "/app/bin does not exist"; \
      exit 1; \
    fi && \
    chmod +x /app/bin/server && \
    chown -R appuser:appgroup /app

USER appuser

# Set environment variables
# FIREBASE_CONFIG_PATH points to the credentials file
# If not set, the app will try Application Default Credentials (works on Cloud Run)
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Use CMD (Cloud Run's __cacert_entrypoint.sh will execute this)
# The __cacert_entrypoint.sh is Cloud Run's wrapper that handles certs and env setup
CMD ["/app/bin/server"]
