# Dockerfile for TickerWire Server
# Uses pre-built artifacts from Gradle installDist

FROM eclipse-temurin:17-jre-jammy

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy pre-built server distribution (built by Gradle installDist)
COPY server/build/install/server/ /app/

# Copy Firebase service account credentials
COPY server/src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json

# Set permissions
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Cloud Run uses PORT environment variable
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json

EXPOSE 8080

# Run the server
CMD ["/app/bin/server"]
