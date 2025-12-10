# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI with proper Android SDK)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create app directory
RUN mkdir -p /app && chown -R appuser:appgroup /app

WORKDIR /app

# Copy pre-built server distribution (prepared in CI)
# We use a custom directory 'server-dist' to avoid .dockerignore issues with 'server/build'
COPY server-dist/ /app/

# Set permissions
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Set environment variables
ENV PORT=8080
# The key is copied as part of server-dist/service-account.json
ENV FIREBASE_CONFIG_PATH=/app/service-account.json

EXPOSE 8080

CMD ["/app/bin/server"]
