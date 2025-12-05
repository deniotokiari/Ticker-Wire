package pl.deniotokiari.tickerwire.common.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * HTTP client wrapper for making network requests.
 * Uses Ktor with JSON serialization support.
 */
@Single
class HttpClient {
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    @PublishedApi
    internal val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.NONE // Set to LogLevel.BODY for debugging
        }
    }

    /**
     * Perform a GET request and deserialize the response.
     *
     * @param url The URL to request
     * @param queryParams Optional query parameters
     * @return The deserialized response body
     */
    suspend inline fun <reified T> get(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
    ): T {
        return client.get(url) {
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    /**
     * Perform a POST request with a JSON body and deserialize the response.
     *
     * @param url The URL to request
     * @param body The request body (will be serialized to JSON)
     * @param queryParams Optional query parameters
     * @return The deserialized response body
     */
    suspend inline fun <reified T, reified R> post(
        url: String,
        body: T,
        queryParams: Map<String, String> = emptyMap(),
    ): R {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    /**
     * Perform a POST request without a body and deserialize the response.
     *
     * @param url The URL to request
     * @param queryParams Optional query parameters
     * @return The deserialized response body
     */
    suspend inline fun <reified R> postEmpty(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
    ): R {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    /**
     * Close the HTTP client.
     * Call this when the client is no longer needed.
     */
    fun close() {
        client.close()
    }
}

