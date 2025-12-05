package pl.deniotokiari.tickerwire.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

private val logger = LoggerFactory.getLogger("RequestLogging")

/**
 * Configure request logging with request IDs for tracing.
 * This adds:
 * - Unique request IDs for correlation
 * - Request/response logging with timing
 * - MDC context for structured logging
 */
fun Application.configureRequestLogging() {
    // Intercept all requests for logging
    intercept(ApplicationCallPipeline.Monitoring) {
        val startTime = System.currentTimeMillis()
        val path = call.request.path()
        
        // Skip health check endpoints to reduce log noise
        if (path.startsWith("/health") || path.startsWith("/ready")) {
            proceed()
            return@intercept
        }
        
        // Generate or retrieve request ID
        val requestId = call.request.headers["X-Request-Id"]
            ?: UUID.randomUUID().toString().take(8)
        
        // Set request ID in MDC for logging context
        MDC.put("request_id", requestId)
        
        // Add request ID to response header
        call.response.headers.append("X-Request-Id", requestId)
        
        val method = call.request.httpMethod.value
        
        try {
            proceed()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val status = call.response.status()?.value ?: "unknown"
            
            logger.info("$method $path -> $status (${duration}ms)")
            
            // Clear MDC
            MDC.remove("request_id")
        }
    }
}

