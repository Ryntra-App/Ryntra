package com.rinthy.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModrinthApiTest {
    @Test
    fun currentAccountIsDecodedAndTokenIsSent() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v2/user", request.url.encodedPath)
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            respond(
                content = """{"id":"user-1","username":"alex","unknown":"ignored"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val account = api.getCurrentAccount("mrp_test")

        assertEquals("user-1", account.id)
        assertEquals("alex", account.username)
        api.close()
    }

    @Test
    fun jwtTokenUsesBearerScheme() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer first.second.third", request.headers[HttpHeaders.Authorization])
            respond(
                content = """{"id":"user-1","username":"alex"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        api.getCurrentAccount("first.second.third")

        api.close()
    }

    @Test
    fun unauthorizedResponseHasSafeActionableMessage() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":"unauthorized","description":"internal detail"}""",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val error = assertFailsWith<ApiException> {
            api.getCurrentAccount("invalid")
        }

        assertEquals(401, error.statusCode)
        assertEquals("The access token is invalid or expired.", error.message)
        api.close()
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        fun testClient(engine: MockEngine) = HttpClient(engine) {
            configureForModrinth()
        }
    }
}
