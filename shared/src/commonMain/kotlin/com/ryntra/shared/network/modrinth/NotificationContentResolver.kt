package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ModrinthNotification
import com.ryntra.shared.model.ModrinthNotificationLink
import com.ryntra.shared.model.Project
import com.ryntra.shared.model.ProjectVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Replaces the raw project and version ids Modrinth puts in notification text with their names.
 *
 * Everything the list mentions is fetched in two bulk requests. Resolving one notification at a
 * time meant one request per notification, in sequence — an inbox of a few hundred kept the
 * notifications screen loading for minutes.
 */
internal class NotificationContentResolver(
    private val projects: ProjectEndpoints,
    private val versions: VersionEndpoints,
) {
    suspend fun resolve(notifications: List<ModrinthNotification>, token: String): List<ModrinthNotification> {
        val references = notifications.associateWith { ModrinthNotificationLink.parse(it.link) }
        val projectReferences = references.mapNotNull { (notification, reference) ->
            reference?.projectIdOrSlug?.takeIf { it in notification.title || it in notification.text }
        }
        val versionIds = references.mapNotNull { (notification, reference) ->
            reference?.versionId?.takeIf { it in notification.title || it in notification.text }
        }
        val (projectsByReference, versionsById) = coroutineScope {
            val projectLookup = async { loadProjects(projectReferences, token) }
            val versionLookup = async { loadVersions(versionIds, token) }
            projectLookup.await() to versionLookup.await()
        }

        return notifications.map { notification ->
            val reference = references[notification] ?: return@map notification
            var title = notification.title
            var text = notification.text
            val project = projectsByReference[reference.projectIdOrSlug.lowercase()]
                ?.takeIf { reference.projectIdOrSlug in title || reference.projectIdOrSlug in text }
            project?.let {
                title = title.replace(reference.projectIdOrSlug, it.title)
                text = text.replace(reference.projectIdOrSlug, it.title)
            }
            var versionTitle: String? = null
            reference.versionId?.let(versionsById::get)?.let { version ->
                val label = version.versionNumber.ifBlank { version.name }
                versionTitle = label
                title = title.replace(version.id, label)
                text = text.replace(version.id, label)
            }
            notification.copy(
                title = title,
                text = text,
                projectTitle = project?.title,
                versionTitle = versionTitle,
            )
        }
    }

    /** Keyed by lower-case id and slug, since a notification link may use either. */
    private suspend fun loadProjects(references: List<String>, token: String): Map<String, Project> =
        bestEffort(references) { chunk -> projects.getMany(chunk, token) }
            .flatMap { project -> listOfNotNull(project.id, project.slug).map { it.lowercase() to project } }
            .toMap()

    private suspend fun loadVersions(versionIds: List<String>, token: String): Map<String, ProjectVersion> =
        bestEffort(versionIds) { chunk -> versions.getMany(chunk, token) }.associateBy(ProjectVersion::id)

    /**
     * Names are cosmetic: a failed lookup leaves the ids in place rather than failing the
     * whole notification list.
     */
    private suspend fun <T> bestEffort(keys: List<String>, fetch: suspend (List<String>) -> List<T>): List<T> =
        keys.distinct().chunked(LOOKUP_CHUNK_SIZE).flatMap { chunk ->
            try {
                fetch(chunk)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
        }

    private companion object {
        /** Keeps the `ids` query parameter well inside URL length limits. */
        const val LOOKUP_CHUNK_SIZE = 100
    }
}
