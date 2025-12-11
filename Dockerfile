# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy pre-built server distribution (built in CI)
COPY server/build/install/server/ /app/

# Copy Firebase credentials
# For local builds: use server/serviceAccountKey.json
# For CI builds: file is created at server/serviceAccountKey.json before Docker build
# Note: Cloud Run can also use Application Default Credentials if this file is not present
COPY server/serviceAccountKey.json /app/serviceAccountKey.json

# Set permissions
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Set environment variables
# FIREBASE_CONFIG_PATH points to the credentials file
# If not set, the app will try Application Default Credentials (works on Cloud Run)
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

CMD ["/app/bin/server"]
