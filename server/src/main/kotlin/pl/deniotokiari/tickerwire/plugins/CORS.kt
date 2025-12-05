package pl.deniotokiari.tickerwire.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CORS")

fun Application.configureCORS() {
    // Read allowed origins from environment variable (comma-separated)
    // Examples:
    //   CORS_ALLOWED_ORIGINS=https://example.com
    //   CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
    //   CORS_ALLOWED_ORIGINS=* (allows any host - use only for development)
    val allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS")
    val allowedOrigins = allowedOriginsEnv
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    val allowAnyHost = allowedOrigins.isEmpty() || allowedOrigins.contains("*")

    if (allowAnyHost) {
        logger.warn("CORS is configured to allow any host. Set CORS_ALLOWED_ORIGINS for production.")
    } else {
        logger.info("CORS allowed origins: $allowedOrigins")
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowCredentials = true

        if (allowAnyHost) {
            anyHost()
        } else {
            allowedOrigins.forEach { origin ->
                allowHost(origin.removePrefix("https://").removePrefix("http://"), schemes = listOf("https", "http"))
            }
        }
    }
}

