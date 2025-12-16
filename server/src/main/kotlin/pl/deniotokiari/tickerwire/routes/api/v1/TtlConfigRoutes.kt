package pl.deniotokiari.tickerwire.routes.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import pl.deniotokiari.tickerwire.services.TtlConfigService

fun Route.ttlConfigRoutes(ttlConfigService: TtlConfigService) {
    route("/api/v1/ttl") {
        get("/client") {
            val ttlConfig = ttlConfigService.ttlConfig.client

            call.respond(HttpStatusCode.OK, ttlConfig)
        }
    }
}
