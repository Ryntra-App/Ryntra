package com.ryntra.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.ryntra.shared.model.CreateVersionRequest
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.ProjectAttentionKind
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.VersionUpdate

class ModrinthApiTest {
    @Test
    fun analyticsV3BucketsAreNormalizedIntoExactProjectMetrics() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v3/analytics", request.url.encodedPath)
            assertTrue(request.bodyText().contains(""""bucket_by":["project_id"]"""))
            respond(
                content = """{
                    "metrics": [
                        [
                            {"metric_kind":"downloads","source_project":"project-1","downloads":42},
                            {"metric_kind":"views","source_project":"project-1","views":125},
                            {"metric_kind":"playtime","source_project":"project-2","seconds":3600}
                        ],
                        [
                            {"metric_kind":"downloads","source_project":"project-1","downloads":8}
                        ]
                    ],
                    "project_events": [
                        {
                            "project_id":"project-1",
                            "timestamp":"2026-07-02T12:00:00Z",
                            "kind":"version_uploaded",
                            "version_id":"version-1",
                            "version_name":"Launch",
                            "version_number":"1.0.0"
                        }
                    ]
                }""".trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val response = api.getAnalytics(
            query = AnalyticsQuery(
                startTime = "2026-07-01T00:00:00Z",
                endTime = "2026-07-03T00:00:00Z",
                slices = 2,
                projectIds = listOf("project-1", "project-2"),
            ),
            includeRevenue = false,
            token = "mrp_test",
        )

        assertEquals(200, response.status)
        assertEquals(50.0, response.points.sumOf { it.metrics.downloads })
        assertEquals(125.0, response.points.sumOf { it.metrics.views })
        assertEquals(3600.0, response.points.sumOf { it.metrics.playtimeSeconds })
        assertEquals(50.0, response.points.sumOf { it.projects["project-1"]?.downloads ?: 0.0 })
        assertEquals("version_uploaded", response.events.single().kind)
        assertEquals("1.0.0", response.events.single().versionNumber)
        api.close()
    }

    @Test
    fun analyticsV3RevenueStringsAreParsed() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v3/analytics", request.url.encodedPath)
            respond(
                content = """{
                    "metrics": [
                        [
                            {"metric_kind":"revenue","source_project":"project-1","user_id":"user-1","revenue":"1.25"},
                            {"metric_kind":"revenue","source_project":"project-1","user_id":"user-2","revenue":"2.75"}
                        ]
                    ],
                    "projects": {},
                    "users": {},
                    "project_events": []
                }""".trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val response = api.getAnalytics(
            query = AnalyticsQuery(
                startTime = "2026-07-01T00:00:00Z",
                endTime = "2026-07-02T00:00:00Z",
                slices = 1,
                projectIds = listOf("project-1"),
            ),
            includeRevenue = true,
            token = "mrp_test",
        )

        assertEquals(4.0, response.points.sumOf { it.metrics.revenue })
        assertEquals(4.0, response.points.sumOf { it.projects["project-1"]?.revenue ?: 0.0 })
        api.close()
    }

    @Test
    fun currentAccountIsDecodedAndTokenIsSent() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v2/user", request.url.encodedPath)
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            respond(
                content = """{
                    "id":"user-1",
                    "username":"alex",
                    "payout_data":{
                        "balance":"12.34",
                        "currency":"USD",
                        "payout_wallet":"paypal",
                        "payout_wallet_type":"email",
                        "payout_address":"alex@example.com"
                    },
                    "unknown":"ignored"
                }""".trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val account = api.getCurrentAccount("mrp_test")

        assertEquals("user-1", account.id)
        assertEquals("alex", account.username)
        assertEquals(12.34, account.payoutData?.balance)
        assertEquals("paypal", account.payoutData?.wallet)
        api.close()
    }

    @Test
    fun walletEndpointsNormalizeBalancesAndTransactions() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/user/user-1/payouts" -> respond(
                    content = """{
                        "all_time":"84.50",
                        "last_month":"6.25",
                        "payouts":[{"created":"2026-06-01T12:00:00Z","amount":"25.00","status":"success"}]
                    }""".trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/v3/payout/balance" -> respond(
                    content = """{
                        "available_now":"10.50",
                        "pending":2.25,
                        "withdrawn_lifetime":"71.75",
                        "currency":"USD"
                    }""".trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                else -> error("Unexpected URL: ${request.url}")
            }
        }
        val api = ModrinthApi(testClient(engine))

        val history = api.getPayoutHistory("user-1", "mrp_test")
        val balance = api.getPayoutBalance("mrp_test")

        assertEquals(84.5, history.allTime)
        assertEquals(6.25, history.lastMonth)
        assertEquals(25.0, history.transactions.single().amount)
        assertEquals(10.5, balance.available)
        assertEquals(2.25, balance.pending)
        assertEquals(71.75, balance.withdrawnLifetime)
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
    fun accountProfileUpdateUsesPatch() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals("/v2/user/user-1", request.url.encodedPath)
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        api.updateAccountProfile(
            userId = "user-1",
            update = com.ryntra.shared.model.AccountProfileUpdate(username = "alex", bio = "hello"),
            token = "mrp_test",
        )

        api.close()
    }

    @Test
    fun projectUpdateUsesPatch() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals("/v2/project/project-1", request.url.encodedPath)
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        api.updateProject(
            projectIdOrSlug = "project-1",
            update = com.ryntra.shared.model.ProjectUpdate(title = "New Title"),
            token = "mrp_test",
        )

        api.close()
    }

    @Test
    fun galleryDeleteUsesEncodedUrlQuery() = runTest {
        val imageUrl = "https://cdn.example/image one.png"
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/v2/project/project-1/gallery", request.url.encodedPath)
            assertEquals(imageUrl, request.url.parameters["url"])
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        val api = ModrinthApi(testClient(engine))

        api.deleteGalleryImage("project-1", imageUrl, "mrp_test")

        api.close()
    }

    @Test
    fun versionCreateUsesMultipartEndpoint() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v2/version", request.url.encodedPath)
            assertTrue(request.body is MultiPartFormDataContent)
            respond(
                content = """{"id":"version-1","project_id":"project-1","name":"Release","version_number":"1.0.0"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val version = api.createVersion(
            projectId = "project-1",
            request = CreateVersionRequest(
                name = "Release",
                versionNumber = "1.0.0",
                gameVersions = listOf("1.21.1"),
                loaders = listOf("fabric"),
                files = listOf(ProjectFileUpload("release.jar", "application/java-archive", byteArrayOf(1, 2, 3))),
            ),
            token = "mrp_test",
        )

        assertEquals("version-1", version.id)
        api.close()
    }

    @Test
    fun versionUpdateAndTeamMemberUpdateUsePatch() = runTest {
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount++
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals(
                if (requestCount == 1) "/v2/version/version-1" else "/v2/team/team-1/members/user-1",
                request.url.encodedPath,
            )
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        val api = ModrinthApi(testClient(engine))

        api.updateVersion("version-1", VersionUpdate(name = "Renamed"), "mrp_test")
        api.updateTeamMember("team-1", "user-1", ProjectMemberUpdate(role = "Developer"), "mrp_test")

        assertEquals(2, requestCount)
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
                      "slug":"ryntra-test",
                      "title":"Ryntra Test",
                      "description":"Short summary",
                      "body":"Full body",
                      "project_type":"mod",
                      "categories":["fabric","utility"],
                      "client_side":"required",
                      "server_side":"optional",
                      "status":"processing",
                      "requested_status":"approved",
                      "queued":"2026-07-13T10:00:00Z",
                      "moderator_message":null,
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

        assertEquals("Ryntra Test", project.title)
        assertEquals("Full body", project.body)
        assertEquals(listOf("fabric", "utility"), project.categories)
        assertEquals("required", project.clientSide)
        assertEquals("processing", project.status)
        assertEquals("approved", project.requestedStatus)
        assertEquals("2026-07-13T10:00:00Z", project.queued)
        assertEquals(null, project.moderatorMessage?.message)
        assertEquals(ProjectAttentionKind.ReviewForPublication, project.attentionState().kind)
        assertTrue(project.attentionState().isInReview)
        assertFalse(project.attentionState().needsAttention)
        assertEquals("MIT", project.license?.name)
        assertEquals("https://cdn.example/image.png", project.gallery.single().url)
        api.close()
    }

    @Test
    fun projectVersionsAreDecoded() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v2/project/project-1/version", request.url.encodedPath)
            respond(
                content = """
                    [
                      {
                        "id":"version-1",
                        "project_id":"project-1",
                        "name":"Launch",
                        "version_number":"1.0.0",
                        "version_type":"release",
                        "game_versions":["1.21.1"],
                        "loaders":["fabric"],
                        "downloads":100,
                        "files":[{"url":"https://cdn.example/file.jar","filename":"file.jar","primary":true}]
                      }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val version = api.getProjectVersions("project-1", "mrp_test").single()

        assertEquals("Launch", version.name)
        assertEquals("1.0.0", version.versionNumber)
        assertEquals(listOf("fabric"), version.loaders)
        assertEquals("file.jar", version.files.single().filename)
        api.close()
    }

    @Test
    fun projectMembersAreDecodedFromTeamEndpoint() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/team/team-1/members" -> respond(
                    content = """
                        [
                          {
                            "team_id":"team-1",
                            "role":"Owner",
                            "is_owner":true,
                            "permissions":1023,
                            "payouts_split":100,
                            "accepted":true,
                            "user":{"id":"user-1","username":"alex","avatar_url":"https://cdn.example/avatar.png","role":"developer"}
                          }
                        ]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                // Roster also merges project/{id}/members.
                "/v2/project/project-1/members" -> respond(
                    content = "[]",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                else -> error("Unexpected path ${request.url.encodedPath}")
            }
        }
        val api = ModrinthApi(testClient(engine))

        val member = api.getProjectMembers("project-1", "team-1", "mrp_test").single()

        assertEquals("alex", member.user.username)
        assertEquals("Owner", member.role)
        assertTrue(member.isOwner)
        assertEquals(1023, member.permissions ?: 0)
        api.close()
    }

    @Test
    fun organizationProjectsDecodeV3NameSummarySchema() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v3/organization/org-1/projects", request.url.encodedPath)
            respond(
                content = """
                    [{
                      "id":"AANobbMI",
                      "slug":"sodium",
                      "name":"Sodium",
                      "summary":"Modern rendering engine",
                      "project_types":["mod"],
                      "loaders":["fabric"],
                      "downloads":100,
                      "followers":10,
                      "status":"approved",
                      "team_id":"team-9",
                      "organization":"org-1",
                      "icon_url":"https://cdn.example/icon.png"
                    }]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val project = api.getOrganizationProjects("org-1", "mrp_test").single()

        assertEquals("Sodium", project.title)
        assertEquals("Modern rendering engine", project.description)
        assertEquals("mod", project.projectType)
        assertEquals(listOf("fabric"), project.loaders)
        assertEquals("team-9", project.team)
        assertEquals("org-1", project.organization)
        api.close()
    }

    @Test
    fun organizationProjectsFallBackFromV3ToV2() = runTest {
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            if (requestCount == 1) {
                assertEquals("/v3/organization/org-1/projects", request.url.encodedPath)
                respond(
                    content = """{"error":"not_found","description":"missing"}""",
                    status = HttpStatusCode.NotFound,
                    headers = jsonHeaders,
                )
            } else {
                assertEquals("/v2/organization/org-1/projects", request.url.encodedPath)
                respond(
                    content = """[{"id":"project-1","title":"Org Project","project_type":"mod"}]""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
            }
        }
        val api = ModrinthApi(testClient(engine))

        val project = api.getOrganizationProjects("org-1", "mrp_test").single()

        assertEquals("Org Project", project.title)
        assertTrue(requestCount >= 2)
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
                    content = """[{"id":"org-1","slug":"ryntra","name":"Ryntra"}]""",
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
    fun projectMembersMergeTeamAndProjectEndpoints() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/team/team-1/members" -> respond(
                    content = """
                        [{
                          "team_id":"team-1",
                          "user":{"id":"u1","username":"alice","avatar_url":null},
                          "role":"Owner",
                          "is_owner":true,
                          "permissions":1023,
                          "accepted":true,
                          "ordering":0
                        },{
                          "team_id":"team-1",
                          "user":{"id":"u2","username":"bob","avatar_url":null},
                          "role":"Dev",
                          "is_owner":false,
                          "permissions":1,
                          "accepted":false,
                          "ordering":1
                        }]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/v2/project/proj-1/members" -> respond(
                    content = """
                        [{
                          "team_id":"team-1",
                          "user":{"id":"u1","username":"alice","avatar_url":null},
                          "role":"Owner",
                          "is_owner":true,
                          "permissions":1023,
                          "accepted":true,
                          "ordering":0
                        }]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                else -> respond("{}", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            }
        }
        val api = ModrinthApi(testClient(engine))
        val members = api.getProjectMembers("proj-1", "team-1", "mrp_test")
        assertEquals(2, members.size)
        assertEquals("alice", members.first().user.username)
        assertTrue(members.any { it.user.username == "bob" && !it.accepted })
        api.close()
    }

    @Test
    fun organizationDetailDecodesMembersWithNullPermissions() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v3/organization/org-1", request.url.encodedPath)
            respond(
                content = """
                    {
                      "id":"org-1",
                      "slug":"ryntra",
                      "name":"Ryntra",
                      "team_id":"team-9",
                      "description":"Creator org",
                      "members":[
                        {
                          "team_id":"team-9",
                          "user":{"id":"u1","username":"alice","avatar_url":"https://cdn.example/a.png"},
                          "role":"Owner",
                          "is_owner":true,
                          "permissions":null,
                          "organization_permissions":null,
                          "accepted":true,
                          "ordering":0
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))
        val organization = api.getOrganization("org-1", "mrp_test")
        assertEquals("Ryntra", organization.name)
        assertEquals(1, organization.members.size)
        assertEquals("alice", organization.members.single().user.username)
        assertTrue(organization.members.single().isOwner)
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

private fun io.ktor.client.request.HttpRequestData.bodyText(): String =
    (body as? io.ktor.http.content.TextContent)?.text.orEmpty()
