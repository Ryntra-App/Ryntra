package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModrinthNotificationLink
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectVersion

internal class NotificationContentResolver(
    private val projects: ProjectEndpoints,
    private val versions: VersionEndpoints,
) {
    suspend fun resolve(notifications: List<ModrinthNotification>, token: String): List<ModrinthNotification> {
        val projectCache = mutableMapOf<String, Project?>()
        val versionCache = mutableMapOf<String, ProjectVersion?>()
        return notifications.map { notification ->
            val reference = ModrinthNotificationLink.parse(notification.link) ?: return@map notification
            var title = notification.title
            var text = notification.text

            val projectReference = reference.projectIdOrSlug
            val project = if (projectReference in title || projectReference in text) {
                projectCache.getOrPut(projectReference) {
                    runCatching { projects.get(projectReference, token) }.getOrNull()
                }
            } else {
                null
            }
            project?.let {
                    title = title.replace(projectReference, it.title)
                    text = text.replace(projectReference, it.title)
            }
            var versionTitle: String? = null
            if (reference.versionId != null && (reference.versionId in title || reference.versionId in text)) {
                val version = versionCache.getOrPut(reference.versionId) {
                    runCatching { versions.get(reference.versionId, token) }.getOrNull()
                }
                version?.let {
                    val label = it.versionNumber.ifBlank { it.name }
                    versionTitle = label
                    title = title.replace(reference.versionId, label)
                    text = text.replace(reference.versionId, label)
                }
            }
            notification.copy(
                title = title,
                text = text,
                projectTitle = project?.title,
                versionTitle = versionTitle,
            )
        }
    }
}
