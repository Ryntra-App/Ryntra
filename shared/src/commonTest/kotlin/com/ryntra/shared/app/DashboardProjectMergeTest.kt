package com.ryntra.shared.app

import com.ryntra.shared.model.Account
import com.ryntra.shared.model.Dashboard
import com.ryntra.shared.model.Project
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
}
