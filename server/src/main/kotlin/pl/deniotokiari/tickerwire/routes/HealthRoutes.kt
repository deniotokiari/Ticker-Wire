package pl.deniotokiari.tickerwire.routes

import com.google.cloud.firestore.Firestore
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class ReadinessResponse(
    val status: String,
    val checks: Map<String, CheckResult>,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class CheckResult(
    val status: String,
    val message: String? = null,
)

/**
 * Health check routes for Cloud Run and load balancers
 *
 * - /health - Simple liveness check (always returns 200 if server is running)
 * - /ready - Readiness check (validates dependencies like Firestore)
 */
fun Route.healthRoutes() {
    val firestore by inject<Firestore>()

    // Liveness probe - simple check that server is running
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            HealthResponse(status = "UP")
        )
    }

    // Readiness probe - checks that dependencies are available
    get("/ready") {
        val checks = mutableMapOf<String, CheckResult>()
        var allHealthy = true

        // Check Firestore connectivity
        try {
            // Simple read operation to verify connection
            firestore.collection("health_check").limit(1).get().get()
            checks["firestore"] = CheckResult(status = "UP")
        } catch (e: Exception) {
            checks["firestore"] = CheckResult(
                status = "DOWN",
                message = e.message?.take(100) // Limit error message length
            )
            allHealthy = false
        }

        val statusCode = if (allHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        val status = if (allHealthy) "UP" else "DOWN"

        call.respond(
            statusCode,
            ReadinessResponse(
                status = status,
                checks = checks,
            )
        )
    }
}

