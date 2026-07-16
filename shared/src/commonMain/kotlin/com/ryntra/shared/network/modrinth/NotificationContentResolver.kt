package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ModrinthNotification
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
            val reference = NotificationReference.parse(notification.link) ?: return@map notification
            var title = notification.title
            var text = notification.text

            if (reference.projectId != null && (reference.projectId in title || reference.projectId in text)) {
                val project = projectCache.getOrPut(reference.projectId) {
                    runCatching { projects.get(reference.projectId, token) }.getOrNull()
                }
                project?.let {
                    title = title.replace(reference.projectId, it.title)
                    text = text.replace(reference.projectId, it.title)
                }
            }
            if (reference.versionId != null && (reference.versionId in title || reference.versionId in text)) {
                val version = versionCache.getOrPut(reference.versionId) {
                    runCatching { versions.get(reference.versionId, token) }.getOrNull()
                }
                version?.let {
                    val label = it.versionNumber.ifBlank { it.name }
                    title = title.replace(reference.versionId, label)
                    text = text.replace(reference.versionId, label)
                }
            }
            if (title == notification.title && text == notification.text) notification else notification.copy(title = title, text = text)
        }
    }
}

private data class NotificationReference(
    val projectId: String?,
    val versionId: String?,
) {
    companion object {
        fun parse(link: String): NotificationReference? {
            val path = link
                .substringAfter("modrinth.com/", link)
                .substringBefore('?')
                .trim('/')
            val segments = path.split('/').filter(String::isNotBlank)
            if (segments.size < 2) return null
            val versionMarker = segments.indexOf("version")
            return NotificationReference(
                projectId = segments.getOrNull(1),
                versionId = versionMarker.takeIf { it >= 0 }?.let { segments.getOrNull(it + 1) },
            )
        }
    }
}
