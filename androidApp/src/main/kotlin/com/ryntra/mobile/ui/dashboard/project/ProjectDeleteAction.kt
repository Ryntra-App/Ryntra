package com.ryntra.mobile.ui.dashboard.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.ryntra.mobile.R

@Composable
internal fun ProjectDeleteAction(
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.project_action_delete_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        FilledTonalButton(
            onClick = onDelete,
            enabled = enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.project_action_delete),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
