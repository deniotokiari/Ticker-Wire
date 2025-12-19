package pl.deniotokiari.tickerwire

import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import pl.deniotokiari.tickerwire.config.FirebaseConfig
import pl.deniotokiari.tickerwire.plugins.configureCORS
import pl.deniotokiari.tickerwire.plugins.configureContentNegotiation
import pl.deniotokiari.tickerwire.plugins.configureKoin
import pl.deniotokiari.tickerwire.plugins.configureRequestLogging
import pl.deniotokiari.tickerwire.plugins.configureScheduledTasks
import pl.deniotokiari.tickerwire.plugins.configureStatusPages
import pl.deniotokiari.tickerwire.routes.configureRouting

private const val PORT = "PORT"
private const val DEFAULT_PORT = 8080
private const val DEFAULT_HOST = "0.0.0.0"

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    // Cloud Run provides PORT environment variable
    val port = System.getenv(PORT)?.toIntOrNull() ?: DEFAULT_PORT

    logger.info("Starting server on port $port")

    embeddedServer(
        factory = Netty,
        port = port,
        host = DEFAULT_HOST,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    FirebaseConfig.initialize()

    // Configure Koin DI (should be configured first)
    configureKoin()

    // Configure plugins
    configureRequestLogging()
    configureContentNegotiation()
    configureCORS()
    configureStatusPages()

    // Configure scheduled tasks (cache cleanup, etc.)
    configureScheduledTasks()

    // Configure routes
    configureRouting()

    // Configure graceful shutdown
    configureGracefulShutdown()
}

/**
 * Configure graceful shutdown handling.
 * This ensures resources are properly cleaned up when the server stops.
 */
private fun Application.configureGracefulShutdown() {
    val httpClient by inject<HttpClient>()

    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")

        try {
            // Close HTTP client connections
            httpClient.close()
            logger.info("HTTP client closed")
        } catch (e: Exception) {
            logger.error("Error closing HTTP client", e)
        }

        logger.info("Graceful shutdown completed")
    }
}
