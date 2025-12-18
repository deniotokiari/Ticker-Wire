package pl.deniotokiari.tickerwire.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import pl.deniotokiari.tickerwire.models.ErrorResponse
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.routes.api.v1.RequestValidationException

private val logger = LoggerFactory.getLogger("StatusPages")

// Check if running in development mode
private val isDevelopment = System.getenv("KTOR_DEVELOPMENT")?.toBooleanStrictOrNull() ?: false

/**
 * Helper to get request ID from MDC (set by RequestLogging plugin)
 */
private fun getRequestId(): String? = MDC.get("request_id")

fun Application.configureStatusPages() {
    install(StatusPages) {
        // Request validation errors (from TickerRoutes)
        exception<RequestValidationException> { call, cause ->
            val requestId = getRequestId()
            logger.warn("Request validation failed [requestId=$requestId]: ${cause.message}")

            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = HttpStatusCode.BadRequest.value,
                    message = cause.message ?: "Request validation failed",
                    error = "ValidationError",
                    requestId = requestId,
                )
            )
        }

        exception<ValidationException> { call, cause ->
            val requestId = getRequestId()
            logger.warn("Validation failed [requestId=$requestId]: ${cause.message}")

            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = HttpStatusCode.BadRequest.value,
                    message = cause.message ?: "Validation failed",
                    error = "ValidationError",
                    requestId = requestId,
                )
            )
        }

        exception<NotFoundException> { call, cause ->
            val requestId = getRequestId()

            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    status = HttpStatusCode.NotFound.value,
                    message = cause.message ?: "Resource not found",
                    error = "NotFound",
                    requestId = requestId,
                )
            )
        }

        // No available provider exception (all providers are at rate limit)
        exception<NoAvailableProviderException> { call, cause ->
            val requestId = getRequestId()
            logger.warn("No available provider [requestId=$requestId]: ${cause.message}")

            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ErrorResponse(
                    status = HttpStatusCode.ServiceUnavailable.value,
                    message = cause.message
                        ?: "Service temporarily unavailable. Please try again later.",
                    error = "ServiceUnavailable",
                    requestId = requestId,
                )
            )
        }

        // Rate limit exceeded
        exception<RateLimitException> { call, cause ->
            val requestId = getRequestId()
            logger.warn("Rate limit exceeded [requestId=$requestId]: ${cause.message}")

            call.respond(
                HttpStatusCode.TooManyRequests,
                ErrorResponse(
                    status = HttpStatusCode.TooManyRequests.value,
                    message = cause.message ?: "Rate limit exceeded. Please try again later.",
                    error = "RateLimitExceeded",
                    requestId = requestId,
                )
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            val requestId = getRequestId()
            logger.warn("Bad request [requestId=$requestId]: ${cause.message}")

            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = HttpStatusCode.BadRequest.value,
                    message = cause.message ?: "Invalid request",
                    error = "BadRequest",
                    requestId = requestId,
                )
            )
        }

        exception<Throwable> { call, cause ->
            val requestId = getRequestId()
            logger.error("Unhandled exception [requestId=$requestId]", cause)

            // In development, include more details; in production, hide them
            val message = if (isDevelopment) {
                cause.message ?: "An internal server error occurred"
            } else {
                "An internal server error occurred"
            }

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    status = HttpStatusCode.InternalServerError.value,
                    message = message,
                    error = "InternalServerError",
                    requestId = requestId,
                )
            )
        }
    }
}

// Custom exceptions for better error handling
class ValidationException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class NoAvailableProviderException(
    message: String = "No available provider at this time",
    val provider: Provider,
) : Exception(message)

class RateLimitException(message: String = "Rate limit exceeded") : Exception(message)

