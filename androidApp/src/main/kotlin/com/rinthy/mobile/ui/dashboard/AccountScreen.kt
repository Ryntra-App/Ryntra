package com.rinthy.mobile.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rinthy.shared.model.Account
import com.rinthy.mobile.ui.components.RinthySecondaryButton
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide

@Composable
fun AccountScreen(
    account: Account,
    projectCount: Int,
    organizationCount: Int,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(account.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = account.role.replaceFirstChar(Char::uppercase),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        val bio = account.bio
        if (!bio.isNullOrBlank()) {
            Text(
                text = bio,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
        Column(modifier = Modifier.padding(top = 28.dp)) {
            Text("Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AccountStat("Projects", projectCount.toString())
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AccountStat("Organizations", organizationCount.toString())
        }
        RinthySecondaryButton(
            text = "Sign out",
            icon = Lucide.LogOut,
            onClick = onSignOut,
            isDestructive = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        )
        Text(
            text = "Rinthy 3.0 · Unofficial Modrinth client",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun AccountStat(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
