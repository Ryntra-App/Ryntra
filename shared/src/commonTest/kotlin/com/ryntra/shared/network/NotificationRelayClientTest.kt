package com.ryntra.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationRelayClientTest {
    @Test
    fun registrationSendsPushTokenWithoutInstallationSecret() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/notifications/installations", request.url.encodedPath)
            assertEquals(null, request.headers["X-Installation-Secret"])
            assertTrue(request.bodyText().contains("\"platform\":\"android\""))
            assertTrue(request.bodyText().contains("\"pushToken\":\"fcm-token-value-that-is-long-enough\""))
            assertTrue(request.bodyText().contains("\"locale\":\"en\""))
            respond(
                """{"installationSecret":"device-secret","isCreated":true}""",
                HttpStatusCode.Created,
                jsonHeaders,
            )
        }
        val client = NotificationRelayClient("https://auth.example.com", testClient(engine))

        val result = client.registerInstallation(
            installationId = "installation_identifier_1234",
            platform = "android",
            pushToken = "fcm-token-value-that-is-long-enough",
        )

        assertEquals("device-secret", result.installationSecret)
        assertTrue(result.isCreated)
    }

    @Test
    fun enrollmentAuthenticatesDeviceAndReturnsAuthorizationUrl() = runTest {
        val engine = MockEngine { request ->
            assertEquals("device-secret", request.headers["X-Installation-Secret"])
            assertTrue(request.bodyText().contains("\"clientState\":\"client_state_value_1234567890\""))
            respond(
                """{"authorizationUrl":"https://auth.example.com/start","expiresIn":600}""",
                HttpStatusCode.Created,
                jsonHeaders,
            )
        }
        val client = NotificationRelayClient("https://auth.example.com", testClient(engine))

        val result = client.createEnrollment(
            installationId = "installation_identifier_1234",
            secret = "device-secret",
            clientState = "client_state_value_1234567890",
        )

        assertEquals("https://auth.example.com/start", result.authorizationUrl)
        assertEquals(600, result.expiresIn)
    }

    @Test
    fun backendErrorsExposeSafeMessage() = runTest {
        val engine = MockEngine {
            respond(
                """{"error":"invalid_installation_credentials","message":"Internal detail"}""",
                HttpStatusCode.Unauthorized,
                jsonHeaders,
            )
        }
        val client = NotificationRelayClient("https://auth.example.com", testClient(engine))

        val failure = assertFailsWith<ApiException> {
            client.getStatus("installation_identifier_1234", "wrong-secret")
        }

        assertEquals(401, failure.statusCode)
        assertEquals("This device is no longer authorized for instant notifications.", failure.message)
    }

    @Test
    fun statusExposesWhyDeliveryNeedsAttention() = runTest {
        val engine = MockEngine {
            respond(
                """{"isConnected":false,"platform":"android","disabledReason":"authorization_expired"}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }
        val client = NotificationRelayClient("https://auth.example.com", testClient(engine))

        val status = client.getStatus("installation_identifier_1234", "device-secret")

        assertEquals(false, status.isConnected)
        assertEquals("authorization_expired", status.disabledReason)
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        fun testClient(engine: MockEngine) = HttpClient(engine) {
            install(ContentNegotiation) { json(apiJson) }
        }
    }
}

private fun HttpRequestData.bodyText(): String = (body as? TextContent)?.text.orEmpty()
