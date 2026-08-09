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
import com.ryntra.shared.model.CreateProjectRequest
import com.ryntra.shared.model.AnalyticsQuery
import com.ryntra.shared.model.ProjectAttentionKind
import com.ryntra.shared.model.ProjectFileUpload
import com.ryntra.shared.model.ProjectMemberUpdate
import com.ryntra.shared.model.VersionUpdate
import com.ryntra.shared.data.DashboardRepository

class ModrinthApiTest {
    @Test
    fun licenseTagsUseShortValueAsProjectLicenseId() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v2/tag/license", request.url.encodedPath)
            respond(
                content = """[{"short":"MIT","name":"MIT License"}]""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val license = api.getLicenses().single()

        assertEquals("MIT", license.id)
        assertEquals("MIT License", license.name)
        api.close()
    }

    @Test
    fun projectCreateUsesMultipartDraftPayload() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v2/project", request.url.encodedPath)
            assertTrue(request.body is MultiPartFormDataContent)
            respond(
                content = """{"id":"project-1","slug":"ryntra-tools","title":"Ryntra Tools","status":"draft"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val project = api.createProject(
            CreateProjectRequest(
                slug = "ryntra-tools", title = "Ryntra Tools", description = "Creator tools",
                body = "# About", projectType = "mod", categories = listOf("utility"),
                clientSide = "required", serverSide = "optional", licenseId = "MIT",
            ),
            "mrp_test",
        )

        assertEquals("draft", project.status)
        api.close()
    }

    @Test
    fun projectDeleteUsesAuthenticatedProjectEndpoint() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/v2/project/project-1", request.url.encodedPath)
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
            )
        }
        val api = ModrinthApi(testClient(engine))

        api.deleteProject("project-1", "mrp_test")

        api.close()
    }
    @Test
    fun versionOnlyDependencyIsResolvedToProjectMetadataInBatches() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/versions" -> {
                    assertEquals("[\"version-1\"]", request.url.parameters["ids"])
                    respond(
                        content = """[{"id":"version-1","project_id":"project-1","name":"Release","version_number":"1.0.0"}]""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                "/v2/projects" -> {
                    assertEquals("[\"project-1\"]", request.url.parameters["ids"])
                    respond(
                        content = """[{"id":"project-1","title":"Sodium","icon_url":"https://cdn.example/sodium.png"}]""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                else -> error("Unexpected URL: ${request.url}")
            }
        }
        val api = ModrinthApi(testClient(engine))

        val dependency = DashboardRepository(api).enrichDependencies(
            listOf(com.ryntra.shared.model.ProjectDependency(versionId = "version-1")),
            "mrp_test",
        ).single()

        assertEquals("Sodium", dependency.title)
        assertEquals("https://cdn.example/sodium.png", dependency.iconUrl)
        api.close()
    }
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
    fun notificationsAreDecodedAndSortedNewestFirst() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v2/user/user-1/notifications", request.url.encodedPath)
            respond(
                content = """[
                    {
                        "id":"older",
                        "user_id":"user-1",
                        "type":"team_invite",
                        "title":"Invitation",
                        "text":"Join the team",
                        "link":"team/team-1",
                        "read":false,
                        "created":"2026-07-01T10:00:00Z",
                        "actions":[{"title":"Accept","action_route":["POST","team/team-1/join"]}]
                    },
                    {
                        "id":"newer",
                        "user_id":"user-1",
                        "type":"status_change",
                        "title":"Approved",
                        "text":"Your project was approved",
                        "link":"mod/project-1",
                        "read":true,
                        "created":"2026-07-02T10:00:00Z",
                        "actions":[]
                    }
                ]""".trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        val notifications = api.getNotifications("user-1", "mrp_test")

        assertEquals(listOf("newer", "older"), notifications.map { it.id })
        assertEquals("POST", notifications.last().actions.single().actionRoute.first())
        api.close()
    }

    @Test
    fun markingNotificationsReadUsesJsonIdsQuery() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals("/v2/notifications", request.url.encodedPath)
            assertEquals("[\"one\",\"two\"]", request.url.parameters["ids"])
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        val api = ModrinthApi(testClient(engine))

        api.markNotificationsRead(listOf("one", "two", "one", ""), "mrp_test")

        api.close()
    }

    @Test
    fun moderationThreadSupportsReadingReplyingAndDeleting() = runTest {
        var requestIndex = 0
        val engine = MockEngine { request ->
            requestIndex++
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            when (requestIndex) {
                1 -> {
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/v2/thread/thread-1", request.url.encodedPath)
                    respond(
                        content = """{
                            "id":"thread-1",
                            "type":"project",
                            "project_id":"project-1",
                            "report_id":null,
                            "messages":[
                                {
                                    "id":"message-1",
                                    "author_id":"user-1",
                                    "body":{"type":"text","body":"Fixed in **1.0.1**","private":false,"replying_to":null},
                                    "created":"2026-07-17T10:00:00Z"
                                },
                                {
                                    "id":"message-2",
                                    "author_id":null,
                                    "body":{"type":"status_change","old_status":"processing","new_status":"approved"},
                                    "created":"2026-07-17T11:00:00Z"
                                }
                            ],
                            "members":[{"id":"user-1","username":"alex","avatar_url":null,"role":"developer"}]
                        }""".trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                2 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/v2/thread/thread-1", request.url.encodedPath)
                    val body = request.bodyText()
                    assertTrue(body.contains("\"type\":\"text\""))
                    assertTrue(body.contains("\"body\":\"Thanks\""))
                    assertTrue(body.contains("\"replying_to\":\"message-1\""))
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
                else -> {
                    assertEquals(HttpMethod.Delete, request.method)
                    assertEquals("/v2/message/message-1", request.url.encodedPath)
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
            }
        }
        val api = ModrinthApi(testClient(engine))

        val thread = api.getModerationThread("thread-1", "mrp_test")
        assertEquals("project-1", thread.projectId)
        assertEquals("alex", thread.authorOf(thread.messages.first())?.username)
        assertEquals("approved", thread.messages.last().body.newStatus)

        api.replyToModerationThread("thread-1", "Thanks", "message-1", "mrp_test")
        api.deleteModerationMessage("message-1", "mrp_test")

        assertEquals(3, requestIndex)
        api.close()
    }

    @Test
    fun notificationIdsAreResolvedToProjectAndVersionNames() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/user/user-1/notifications" -> respond(
                    content = """[{
                        "id":"notice-1",
                        "user_id":"user-1",
                        "type":"project_update",
                        "title":"project-1 released version-1",
                        "text":"Download version-1 for project-1",
                        "link":"mod/project-1/version/version-1",
                        "read":false,
                        "created":"2026-07-02T10:00:00Z",
                        "actions":[]
                    }]""".trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/v2/project/project-1" -> respond(
                    content = """{"id":"project-1","slug":"ryntra","title":"Ryntra"}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/v2/version/version-1" -> respond(
                    content = """{
                        "id":"version-1",
                        "project_id":"project-1",
                        "name":"Ryntra 3.0",
                        "version_number":"3.0.0",
                        "version_type":"release",
                        "game_versions":[],
                        "loaders":[],
                        "downloads":0,
                        "dependencies":[],
                        "files":[]
                    }""".trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                else -> respond("{}", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            }
        }
        val api = ModrinthApi(testClient(engine))

        val notification = api.getNotifications("user-1", "mrp_test").single()

        assertEquals("Ryntra released 3.0.0", notification.title)
        assertEquals("Download 3.0.0 for Ryntra", notification.text)
        api.close()
    }

    @Test
    fun projectUpdateUsesPatch() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals("/v2/project/project-1", request.url.encodedPath)
            assertEquals("mrp_test", request.headers[HttpHeaders.Authorization])
            assertTrue(request.bodyText().contains("\"license_id\":\"LicenseRef-Custom\""))
            assertTrue(request.bodyText().contains("\"license_url\":\"https://example.com/license\""))
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
                headers = jsonHeaders,
            )
        }
        val api = ModrinthApi(testClient(engine))

        api.updateProject(
            projectIdOrSlug = "project-1",
            update = com.ryntra.shared.model.ProjectUpdate(
                title = "New Title",
                licenseId = "LicenseRef-Custom",
                licenseUrl = "https://example.com/license",
            ),
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
        assertEquals("unauthorized", error.errorCode)
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
