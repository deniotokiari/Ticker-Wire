package pl.deniotokiari.tickerwire.routes.api.v1

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import pl.deniotokiari.tickerwire.adapter.StockProvider

// Request limits
private const val MAX_TICKERS_PER_REQUEST = 50
private const val MAX_QUERY_LENGTH = 100
private const val MAX_TICKER_LENGTH = 10

// Ticker format: alphanumeric, dots, and hyphens (e.g., BRK.A, BRK-B)
private val TICKER_PATTERN = Regex("^[A-Za-z0-9.-]+$")

class RequestValidationException(message: String) : IllegalArgumentException(message)

/**
 * Validates a search query string
 */
private fun validateSearchQuery(query: String?): String {
    if (query == null) {
        throw RequestValidationException("Query parameter is required")
    }

    val trimmedQuery = query.trim()

    if (trimmedQuery.isBlank()) {
        throw RequestValidationException("Query cannot be empty")
    }

    if (trimmedQuery.length > MAX_QUERY_LENGTH) {
        throw RequestValidationException("Query exceeds maximum length of $MAX_QUERY_LENGTH characters")
    }

    return trimmedQuery
}

/**
 * Validates a list of ticker symbols
 */
private fun validateTickerList(tickers: List<String>): List<String> {
    if (tickers.isEmpty()) {
        throw RequestValidationException("Ticker list cannot be empty")
    }

    if (tickers.size > MAX_TICKERS_PER_REQUEST) {
        throw RequestValidationException("Request exceeds maximum of $MAX_TICKERS_PER_REQUEST tickers")
    }

    return tickers.map { ticker ->
        val trimmedTicker = ticker.trim().uppercase()

        if (trimmedTicker.isBlank()) {
            throw RequestValidationException("Ticker symbol cannot be empty")
        }

        if (trimmedTicker.length > MAX_TICKER_LENGTH) {
            throw RequestValidationException("Ticker '$trimmedTicker' exceeds maximum length of $MAX_TICKER_LENGTH characters")
        }

        if (!TICKER_PATTERN.matches(trimmedTicker)) {
            throw RequestValidationException("Ticker '$trimmedTicker' contains invalid characters")
        }

        trimmedTicker
    }.distinct() // Remove duplicates
}

fun Route.tickerRoutes(stockProvider: StockProvider) {
    route("/api/v1/tickers") {
        get("/search") {
            val query = validateSearchQuery(call.request.queryParameters["query"])
            val results = stockProvider.search(query)

            call.respond(HttpStatusCode.OK, results)
        }

        post("/news") {
            val request = call.receive<List<String>>()
            val validatedTickers = validateTickerList(request)
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val result = stockProvider.news(validatedTickers, limit)

            call.respond(HttpStatusCode.OK, result)
        }

        post("/info") {
            val request = call.receive<List<String>>()
            val validatedTickers = validateTickerList(request)
            val result = stockProvider.info(validatedTickers)

            call.respond(HttpStatusCode.OK, result)
        }
    }
}

