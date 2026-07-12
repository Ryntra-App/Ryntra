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
import kotlin.test.assertTrue

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
    fun projectDetailsAreDecoded() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v2/project/project-1", request.url.encodedPath)
            respond(
                content = """
                    {
                      "id":"project-1",
                      "slug":"rinthy-test",
                      "title":"Rinthy Test",
                      "description":"Short summary",
                      "body":"Full body",
                      "project_type":"mod",
                      "categories":["fabric","utility"],
                      "client_side":"required",
                      "server_side":"optional",
                      "downloads":42,
                      "followers":7,
                      "license":{"id":"mit","name":"MIT"},
                      "gallery":[{"url":"https://cdn.example/image.png","featured":true}]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val project = api.getProject("project-1", "mrp_test")

        assertEquals("Rinthy Test", project.title)
        assertEquals("Full body", project.body)
        assertEquals(listOf("fabric", "utility"), project.categories)
        assertEquals("required", project.clientSide)
        assertEquals("MIT", project.license?.name)
        assertEquals("https://cdn.example/image.png", project.gallery.single().url)
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

    @Test
    fun organizationsFallBackFromV3ToV2() = runTest {
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            when (request.url.encodedPath) {
                "/v3/user/user-1/organizations" -> respond(
                    content = """{"error":"not_found","description":"The requested route does not exist."}""",
                    status = HttpStatusCode.NotFound,
                    headers = jsonHeaders,
                )
                "/v2/user/user-1/organizations" -> respond(
                    content = """[{"id":"org-1","slug":"rinthy","name":"Rinthy"}]""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                else -> error("Unexpected URL: ${request.url}")
            }
        }
        val api = ModrinthApi(testClient(engine))

        val organizations = api.getOrganizations("user-1", "mrp_test")

        assertEquals(2, requestCount)
        assertEquals("org-1", organizations.single().id)
        api.close()
    }

    @Test
    fun missingOrganizationRoutesReturnEmptyList() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":"not_found","description":"The requested route does not exist."}""",
                status = HttpStatusCode.NotFound,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val organizations = api.getOrganizations("user-1", "mrp_test")

        assertTrue(organizations.isEmpty())
        api.close()
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        fun testClient(engine: MockEngine) = HttpClient(engine) {
            configureForModrinth()
        }
    }
}
