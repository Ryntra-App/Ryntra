package com.ryntra.shared.model

/** A status payload safe to send from the regular project editor. */
data class ProjectVisibilityUpdate(
    val status: String? = null,
    val requestedStatus: String? = null,
)

/**
 * Centralizes Modrinth's split between current visibility (`status`) and the
 * desired visibility of projects that are not yet in the approved family
 * (`requested_status`). Moderation submission is deliberately absent: only the
 * dedicated submission action may set `status=processing`.
 */
object ProjectStatusPolicy {
    val editableVisibilityStatuses: List<String> = listOf(
        "approved",
        "archived",
        "unlisted",
        "private",
        "draft",
    )

    fun updateFor(
        currentStatus: String,
        currentRequestedStatus: String?,
        desiredStatus: String,
    ): ProjectVisibilityUpdate? {
        val current = currentStatus.trim().lowercase()
        val currentRequested = currentRequestedStatus?.trim()?.lowercase()
        val desired = desiredStatus.trim().lowercase()
        if (desired !in editableVisibilityStatuses) return null

        return if (current in DIRECT_STATUS_FAMILY) {
            if (current == desired) null else ProjectVisibilityUpdate(status = desired)
        } else {
            if (currentRequested == desired) null else ProjectVisibilityUpdate(requestedStatus = desired)
        }
    }

    private val DIRECT_STATUS_FAMILY = setOf("approved", "archived", "unlisted", "private")
}

fun Project.visibilityUpdateFor(desiredStatus: String): ProjectVisibilityUpdate? =
    ProjectStatusPolicy.updateFor(status, requestedStatus, desiredStatus)
