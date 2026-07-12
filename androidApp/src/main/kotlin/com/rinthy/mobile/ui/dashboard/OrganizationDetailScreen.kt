package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rinthy.shared.model.Organization
import com.rinthy.shared.model.Project

@Composable
fun OrganizationDetailScreen(
    organization: Organization,
    projects: List<Project>,
    isLoading: Boolean,
    errorMessage: String?,
    onProjectClick: (Project) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
    ) {
        item { OrganizationIdentity(organization) }
        when {
            isLoading -> item { LoadingOrganizationProjects() }
            errorMessage != null && projects.isEmpty() -> item {
                EmptyState(title = "Projects unavailable", message = errorMessage)
            }
            projects.isEmpty() -> item {
                EmptyState(
                    title = "No organization projects",
                    message = "Projects transferred into this organization will appear here.",
                )
            }
            else -> {
                item {
                    Text(
                        text = "${projects.size} projects",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                items(projects, key = Project::id) { project ->
                    ProjectRow(
                        project = project,
                        showStatus = true,
                        onClick = { onProjectClick(project) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun OrganizationIdentity(organization: Organization) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
        AsyncImage(
            model = organization.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f),
        ) {
            Text(
                text = organization.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${organization.slug}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (organization.description.isNotBlank()) {
                Text(
                    text = organization.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingOrganizationProjects() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 28.dp)) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            "Loading projects",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
