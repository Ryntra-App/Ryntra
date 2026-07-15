package com.ryntra.shared.model

enum class ProjectSortMode(
    val label: String,
) {
    Popularity("Popular"),
    Updated("Updated"),
    Title("A-Z"),
    Followers("Followers"),
}

fun List<Project>.sortedForDisplay(
    mode: ProjectSortMode,
    favoriteIds: Set<String> = emptySet(),
): List<Project> {
    val sorted = when (mode) {
        ProjectSortMode.Popularity -> sortedByDescending(Project::downloads)
        ProjectSortMode.Updated -> sortedByDescending { it.updated.orEmpty() }
        ProjectSortMode.Title -> sortedBy { it.title.lowercase() }
        ProjectSortMode.Followers -> sortedByDescending(Project::followers)
    }
    return sorted.sortedByDescending { it.id in favoriteIds }
}
