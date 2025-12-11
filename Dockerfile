# Dockerfile for TickerWire Server
# Uses pre-built artifacts (built in CI)

FROM eclipse-temurin:17-jre-jammy

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Copy pre-built server distribution
COPY --chown=appuser:appgroup server/build/install/server/ /app/

# Copy Firebase credentials
COPY server/serviceAccountKey.json /app/serviceAccountKey.json

# Set permissions
RUN chmod +x /app/bin/server && chown -R appuser:appgroup /app

USER appuser

# Set environment variables
ENV PORT=8080
ENV FIREBASE_CONFIG_PATH=/app/serviceAccountKey.json
ENV APP_HOME=/app

EXPOSE 8080

# Construct classpath using find and run Java
# This is the most reliable way to build the classpath
CMD ["/bin/sh", "-c", "cd /app && CLASSPATH=$(find /app/lib -name '*.jar' | tr '\\n' ':') && exec java -cp \"$CLASSPATH\" pl.deniotokiari.tickerwire.ApplicationKt"]
