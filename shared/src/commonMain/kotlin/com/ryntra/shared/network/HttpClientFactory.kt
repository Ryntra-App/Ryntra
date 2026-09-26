package com.ryntra.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

internal val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = false
}

expect fun createPlatformHttpClient(): HttpClient

internal fun HttpClientConfig<*>.configureForModrinth() {
    install(ContentNegotiation) {
        json(apiJson)
    }
    // Modrinth's API intermittently leaves a request hanging or answers 502 while an
    // identical request a second later returns in half a second. So a read gets a short
    // timeout per attempt and is retried, instead of one long wait the user sits through
    // before seeing an error. Writes are never retried: repeating a create or a
    // withdrawal after a lost response could apply it twice.
    install(HttpRequestRetry) {
        retryIf(MAX_READ_RETRIES) { request, response ->
            request.method in retriedMethods && response.status.value in retriedStatuses
        }
        retryOnExceptionIf(MAX_READ_RETRIES) { request, cause ->
            request.method in retriedMethods && cause.isRetriableFailure()
        }
        exponentialDelay(baseDelayMs = 400, maxDelayMs = 2_000, randomizationMs = 300)
    }
    // Installed after HttpRequestRetry so each attempt gets its own timeout.
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 8_000
        socketTimeoutMillis = 10_000
    }
    defaultRequest {
        url("https://api.modrinth.com/v2/")
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.UserAgent, "Ryntra/3.2.0 (com.ryntra.mobile)")
        }
    }
}

private const val MAX_READ_RETRIES = 2
private val retriedMethods = setOf(HttpMethod.Get, HttpMethod.Head)
private val retriedStatuses = setOf(502, 503, 504)

/**
 * A timeout can surface wrapped in a cancellation, so the whole cause chain is checked; a
 * plain cancellation means the caller gave up and must not be retried.
 */
private fun Throwable.isRetriableFailure(): Boolean {
    val chain = generateSequence(this) { it.cause }.toList()
    if (chain.any { it is HttpRequestTimeoutException || it is ConnectTimeoutException || it is SocketTimeoutException }) {
        return true
    }
    return this !is CancellationException && chain.any { it is IOException }
}
