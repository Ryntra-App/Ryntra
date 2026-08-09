package com.ryntra.shared.app

import com.ryntra.shared.model.Account
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectLicense
import com.ryntra.shared.model.ProjectUpdate
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardProjectMergeTest {
    @Test
    fun freshProjectStatisticsReplaceTheDashboardCopy() {
        val dashboard = Dashboard(
            account = Account(id = "user", username = "creator"),
            projects = listOf(Project(id = "project-id", slug = "project-slug", title = "Project", downloads = 10)),
            organizations = emptyList(),
        )
        val fresh = dashboard.projects.single().copy(downloads = 25, followers = 7)

        val merged = dashboard.withFreshProject(fresh)

        assertEquals(25, merged.projects.single().downloads)
        assertEquals(7, merged.projects.single().followers)
    }

    @Test
    fun licenseChangeDoesNotKeepMetadataFromPreviousLicense() {
        val dashboard = dashboardWith(
            Project(
                id = "project-id",
                title = "Project",
                license = ProjectLicense(
                    id = "LicenseRef-Custom",
                    name = "Custom license",
                    url = "https://example.com/old-license",
                ),
            ),
        )

        val merged = dashboard.withUpdatedProject(
            "project-id",
            ProjectUpdate(licenseId = "MIT"),
        )

        assertEquals(ProjectLicense(id = "MIT"), merged.projects.single().license)
    }

    @Test
    fun customLicenseUrlAndRequestedStatusAreMerged() {
        val dashboard = dashboardWith(Project(id = "project-id", title = "Project", status = "draft"))

        val merged = dashboard.withUpdatedProject(
            "project-id",
            ProjectUpdate(
                requestedStatus = "private",
                licenseId = "LicenseRef-Custom",
                licenseUrl = "https://example.com/license",
            ),
        )

        assertEquals("private", merged.projects.single().requestedStatus)
        assertEquals("https://example.com/license", merged.projects.single().license?.url)
    }

    @Test
    fun directStatusUpdateClearsStaleRequestedStatus() {
        val dashboard = dashboardWith(
            Project(
                id = "project-id",
                title = "Project",
                status = "approved",
                requestedStatus = "private",
            ),
        )

        val merged = dashboard.withUpdatedProject("project-id", ProjectUpdate(status = "unlisted"))

        assertEquals("unlisted", merged.projects.single().status)
        assertEquals(null, merged.projects.single().requestedStatus)
    }

    private fun dashboardWith(project: Project): Dashboard = Dashboard(
        account = Account(id = "user", username = "creator"),
        projects = listOf(project),
        organizations = emptyList(),
    )
}
