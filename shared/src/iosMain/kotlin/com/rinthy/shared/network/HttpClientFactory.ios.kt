package com.rinthy.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin) {
    configureForModrinth()
    engine {
        configureRequest {
            setAllowsCellularAccess(true)
        }
    }
}
