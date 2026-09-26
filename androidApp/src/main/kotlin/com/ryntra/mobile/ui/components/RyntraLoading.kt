package com.ryntra.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryntra.mobile.ui.theme.RyntraDesign

/**
 * The one way a screen or section says "the first load is still running".
 *
 * Material 3 Expressive reserves its morphing [LoadingIndicator] for exactly this — content
 * that is not on screen yet — and keeps the circular indicator for work inside a control.
 * Every first-load spot goes through here so they share one size, one placement and one
 * spoken label instead of each screen picking its own spinner.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun RyntraContentLoading(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (RyntraDesign.isPlatformNative) {
            LoadingIndicator()
        } else {
            RyntraProgressIndicator(RyntraDesign.colors.accent, Modifier.size(28.dp))
        }
        if (label != null) {
            Text(
                text = label,
                color = RyntraDesign.colors.labelSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
