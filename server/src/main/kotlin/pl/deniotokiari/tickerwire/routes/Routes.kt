package pl.deniotokiari.tickerwire.routes

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import pl.deniotokiari.tickerwire.Greeting
import pl.deniotokiari.tickerwire.adapter.StockProvider
import pl.deniotokiari.tickerwire.routes.api.v1.statsRoutes
import pl.deniotokiari.tickerwire.routes.api.v1.tickerRoutes
import pl.deniotokiari.tickerwire.routes.api.v1.ttlConfigRoutes
import pl.deniotokiari.tickerwire.services.TtlConfigService

fun Application.configureRouting() {
    val stockProvider: StockProvider by inject()
    val ttlConfigService: TtlConfigService by inject()

    routing {
        // Root endpoint
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        // Health check endpoints (for Cloud Run / load balancers)
        healthRoutes()

        // API routes
        tickerRoutes(stockProvider)
        ttlConfigRoutes(ttlConfigService)

        // Stats routes
        route("/api/v1") {
            statsRoutes()
        }
    }
}

