# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI with proper Android SDK)

FROM eclipse-temurin:17-jre-jammy

# Install required packages (bash for Gradle script, file for debugging)
RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    file \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy pre-built server distribution (downloaded from CI artifacts to docker-dist/)
COPY docker-dist/ /app/

# Copy Firebase credentials (injected at build time in CI)
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Set permissions
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Set environment variables
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run Java directly with explicit classpath
# Using exec form avoids an extra shell layer and ensures Cloud Run picks it up
ENTRYPOINT ["java", "-cp", "/app/lib/*:/app/bin", "pl.deniotokiari.tickerwire.ApplicationKt"]
