# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy pre-built server distribution (built in CI)
# Copy the entire server directory structure, then move what we need
COPY --chown=appuser:appgroup server/build/install/server/ /tmp/server-dist/
RUN echo "=== Verifying COPY worked ===" && \
    ls -la /tmp/server-dist/ && \
    echo "" && \
    echo "Checking /tmp/server-dist/bin:" && \
    ls -la /tmp/server-dist/bin/ || (echo "ERROR: /tmp/server-dist/bin does not exist!" && exit 1) && \
    echo "" && \
    echo "Checking /tmp/server-dist/bin/server:" && \
    test -f /tmp/server-dist/bin/server || (echo "ERROR: /tmp/server-dist/bin/server does not exist!" && exit 1) && \
    echo "✅ Source files verified" && \
    echo "" && \
    mkdir -p /app/bin /app/lib && \
    echo "Copying bin files..." && \
    cp -v /tmp/server-dist/bin/* /app/bin/ && \
    echo "Copying lib files..." && \
    cp -v /tmp/server-dist/lib/* /app/lib/ && \
    rm -rf /tmp/server-dist && \
    echo "" && \
    echo "=== Verifying copied files ===" && \
    echo "Files in /app/bin:" && \
    ls -la /app/bin/ && \
    echo "" && \
    echo "Files in /app/lib (first 5):" && \
    ls -la /app/lib/ | head -5 && \
    echo "" && \
    test -f /app/bin/server || (echo "ERROR: /app/bin/server not found after copy!" && exit 1) && \
    echo "✅ /app/bin/server exists"

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
    (ls -la /app/lib/ | head -5 || echo "Cannot list /app/lib") && \
    echo "" && \
    echo "File count in /app/bin:" && \
    (find /app/bin -type f | wc -l || echo "0") && \
    echo "File count in /app/lib:" && \
    (find /app/lib -type f | wc -l || echo "0")

# Copy Firebase credentials
# For local builds: use server/serviceAccountKey.json
# For CI builds: file is created at server/serviceAccountKey.json before Docker build
# Note: Cloud Run can also use Application Default Credentials if this file is not present
COPY server/serviceAccountKey.json /app/serviceAccountKey.json

# Verify server binary exists and inspect the script
RUN if [ ! -f /app/bin/server ]; then \
      echo "ERROR: /app/bin/server not found!"; \
      echo "Contents of /app:"; \
      ls -la /app/; \
      echo "Contents of /app/bin (if exists):"; \
      ls -la /app/bin/ 2>/dev/null || echo "/app/bin does not exist"; \
      exit 1; \
    fi && \
    echo "=== Inspecting /app/bin/server script ===" && \
    echo "First 10 lines:" && \
    head -10 /app/bin/server && \
    echo "" && \
    echo "Checking for APP_HOME or similar:" && \
    grep -i "APP_HOME\|APPNAME\|CLASSPATH" /app/bin/server | head -5 || echo "No APP_HOME found" && \
    echo "" && \
    echo "Fixing line endings (CRLF to LF) if needed..." && \
    sed -i 's/\r$//' /app/bin/server && \
    chmod +x /app/bin/server && \
    chown -R appuser:appgroup /app && \
    echo "✅ Script prepared"

USER appuser

# Set environment variables
# FIREBASE_CONFIG_PATH points to the credentials file
# If not set, the app will try Application Default Credentials (works on Cloud Run)
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Set APP_HOME environment variable (Gradle scripts often use this)
ENV APP_HOME=/app
WORKDIR /app

# Run Java directly - most reliable approach for Cloud Run
# No script needed, just execute Java with the classpath
# Cloud Run's __cacert_entrypoint.sh will handle this CMD
CMD ["java", "-cp", "/app/lib/*", "pl.deniotokiari.tickerwire.ApplicationKt"]
