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

# Create a simple wrapper script that constructs classpath from all JARs
# Use a here-document for more reliable script creation (avoids line ending issues)
RUN cat > /app/start-server.sh << 'EOFSCRIPT' && \
#!/bin/bash
set -e
cd /app
echo "Starting TickerWire Server..."
echo "Working directory: $(pwd)"
echo "Java version: $(java -version 2>&1 | head -1)"
JAR_COUNT=$(find /app/lib -name "*.jar" | wc -l)
if [ "$JAR_COUNT" -eq 0 ]; then
  echo "ERROR: No JAR files found in /app/lib!"
  ls -la /app/lib/ || echo "Directory /app/lib does not exist"
  exit 1
fi
echo "Found $JAR_COUNT JAR file(s) in /app/lib"
CLASSPATH=$(find /app/lib -name "*.jar" | tr "\n" ":")
echo "Classpath: $CLASSPATH"
echo "Starting application: pl.deniotokiari.tickerwire.ApplicationKt"
exec java -cp "$CLASSPATH" pl.deniotokiari.tickerwire.ApplicationKt "$@"
EOFSCRIPT
    chmod +x /app/start-server.sh && \
    chown appuser:appgroup /app/start-server.sh && \
    echo "✅ Created start-server.sh wrapper" && \
    echo "Verifying script exists and is readable:" && \
    ls -lh /app/start-server.sh && \
    echo "Script contents (first 5 lines):" && \
    head -5 /app/start-server.sh && \
    echo "Testing script can be read:" && \
    test -r /app/start-server.sh && echo "✅ Script is readable" || echo "❌ Script is NOT readable"

USER appuser

# Set environment variables
# FIREBASE_CONFIG_PATH points to the credentials file
# If not set, the app will try Application Default Credentials (works on Cloud Run)
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Set APP_HOME environment variable (for reference, though we use our wrapper)
ENV APP_HOME=/app
WORKDIR /app

# Verify JARs are present
RUN JAR_COUNT=$(find /app/lib -name "*.jar" | wc -l) && \
    echo "Found $JAR_COUNT JAR file(s) in /app/lib" && \
    if [ "$JAR_COUNT" -eq 0 ]; then echo "ERROR: No JAR files found!" && exit 1; fi && \
    echo "✅ JAR files verified"

# Run Java directly - construct classpath and execute in one command
# Cloud Run's __cacert_entrypoint.sh does `exec "$@"`, so it will execute: /bin/bash -c "..."
# This avoids any script file issues - everything is inline
CMD ["/bin/sh", "-c", "cd /app && CLASSPATH=$(find /app/lib -name '*.jar' | tr '\\n' ':') && exec java -cp \"$CLASSPATH\" pl.deniotokiari.tickerwire.ApplicationKt"]
