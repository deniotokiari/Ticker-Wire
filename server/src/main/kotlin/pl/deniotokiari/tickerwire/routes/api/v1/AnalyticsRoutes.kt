package pl.deniotokiari.tickerwire.routes.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import pl.deniotokiari.tickerwire.services.analytics.ProviderStatsService

/**
 * Provider Stats API routes
 *
 * GET /api/v1/stats - Get current month's provider stats
 * GET /api/v1/stats/{month} - Get stats for specific month (yyyy-MM)
 */
fun Route.statsRoutes() {
    val statsService by inject<ProviderStatsService>()

    route("/stats") {
        // Get current month's stats
        get {
            val stats = statsService.getCurrentMonthStats()
            call.respond(HttpStatusCode.OK, stats)
        }

        // Get stats for a specific month
        get("/{month}") {
            val month = call.parameters["month"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Month parameter required"))

            // Validate format yyyy-MM
            if (!month.matches(Regex("\\d{4}-\\d{2}"))) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid month format. Use yyyy-MM (e.g., 2024-12)")
                )
            }

            val stats = statsService.getStatsForMonth(month)
            call.respond(HttpStatusCode.OK, stats)
        }
    }
}
