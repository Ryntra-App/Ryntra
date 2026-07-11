package com.rinthy.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
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
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 20_000
    }
    defaultRequest {
        url("https://api.modrinth.com/v2/")
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.UserAgent, "Rinthy/3.0.0 (com.rinthy.mobile)")
        }
    }
}
