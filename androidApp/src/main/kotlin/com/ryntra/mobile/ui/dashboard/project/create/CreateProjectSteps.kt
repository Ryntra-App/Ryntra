package com.ryntra.mobile.ui.dashboard.project.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Server
import com.composables.icons.lucide.Tag
import com.ryntra.mobile.R
import com.ryntra.mobile.ui.dashboard.project.markdown.MarkdownEditor
import com.ryntra.shared.model.ProjectCreationMetadata
import com.ryntra.shared.model.ProjectCreationRules
import com.ryntra.shared.model.ProjectCategory
import com.ryntra.shared.model.ProjectLicense

@Composable
internal fun ProjectCreationStepHeader(step: Int) {
    val title = stringResource(
        when (step) {
            0 -> R.string.project_create_step_basics
            1 -> R.string.project_create_step_compatibility
            else -> R.string.project_create_step_page
        },
    )
    val description = stringResource(
        when (step) {
            0 -> R.string.project_create_step_basics_help
            1 -> R.string.project_create_step_compatibility_help
            else -> R.string.project_create_step_page_help
        },
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(R.string.project_create_step_count, step + 1, 3),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ProjectCreationError(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(Lucide.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(10.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun ProjectBasicsStep(
    draft: CreateProjectDraft,
    metadata: ProjectCreationMetadata,
    iconError: String?,
    showValidationErrors: Boolean,
    onChooseIcon: () -> Unit,
) {
    var isTypePickerOpen by rememberSaveable { mutableStateOf(false) }
    val isTitleMissing = showValidationErrors && draft.title.isBlank()
    val isTitleInvalid = draft.title.isNotBlank() && !draft.isTitleValid
    val isSlugMissing = showValidationErrors && draft.slug.isBlank()
    val isSlugInvalid = draft.slug.isNotBlank() && !draft.isSlugValid
    val isSummaryMissing = showValidationErrors && draft.summary.isBlank()
    val isSummaryInvalid = draft.summary.isNotBlank() && !draft.isSummaryValid

    ProjectDraftNotice()

    FormSection(
        title = stringResource(R.string.project_create_identity),
        description = stringResource(R.string.project_create_identity_help),
        contentSpacing = 12.dp,
    ) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = draft::updateTitle,
            label = { Text(stringResource(R.string.project_create_name)) },
            placeholder = { Text(stringResource(R.string.project_create_name_hint)) },
            leadingIcon = { Icon(Lucide.FileText, contentDescription = null) },
            supportingText = {
                Text(
                    if (isTitleMissing) stringResource(R.string.project_create_name_required)
                    else if (isTitleInvalid) stringResource(R.string.project_create_name_error)
                    else stringResource(R.string.project_create_character_count, draft.title.length, ProjectCreationRules.TITLE_MAX_LENGTH),
                )
            },
            isError = isTitleMissing || isTitleInvalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.slug,
            onValueChange = draft::updateSlug,
            label = { Text(stringResource(R.string.project_create_slug)) },
            placeholder = { Text("my-project") },
            leadingIcon = { Icon(Lucide.Hash, contentDescription = null) },
            supportingText = {
                Text(
                    if (isSlugMissing) stringResource(R.string.project_create_slug_required)
                    else if (isSlugInvalid) stringResource(R.string.project_create_slug_error)
                    else if (draft.slug.isNotEmpty()) "modrinth.com/project/${draft.slug}"
                    else stringResource(R.string.project_create_slug_help),
                )
            },
            isError = isSlugMissing || isSlugInvalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.summary,
            onValueChange = { draft.summary = it.take(ProjectCreationRules.DESCRIPTION_MAX_LENGTH + 1) },
            label = { Text(stringResource(R.string.project_create_summary)) },
            placeholder = { Text(stringResource(R.string.project_create_summary_hint)) },
            leadingIcon = { Icon(Lucide.FileText, contentDescription = null) },
            supportingText = {
                Text(
                    if (isSummaryMissing) stringResource(R.string.project_create_summary_required)
                    else if (isSummaryInvalid) stringResource(R.string.project_create_summary_error)
                    else stringResource(R.string.project_create_character_count, draft.summary.length, ProjectCreationRules.DESCRIPTION_MAX_LENGTH),
                )
            },
            isError = isSummaryMissing || isSummaryInvalid,
            minLines = 1,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    FormSection(
        title = stringResource(R.string.project_create_type),
        description = stringResource(R.string.project_create_type_help),
    ) {
        SelectionField(
            icon = Lucide.Tag,
            title = stringResource(R.string.project_create_type),
            value = projectTypeLabel(draft.projectType),
            description = projectTypeDescription(draft.projectType),
            onClick = { isTypePickerOpen = true },
        )
    }
    if (isTypePickerOpen) {
        SingleChoicePickerSheet(
            title = stringResource(R.string.project_create_choose_type),
            description = stringResource(R.string.project_create_choose_type_help),
            values = metadata.projectTypes.filterNot { it == HIDDEN_PROJECT_TYPE },
            selected = draft.projectType,
            labelFor = { projectTypeLabel(it) },
            descriptionFor = { projectTypeDescription(it) },
            onDismiss = { isTypePickerOpen = false },
            onSelect = {
                draft.projectType = it
                draft.categories = emptySet()
                isTypePickerOpen = false
            },
        )
    }

    FormSection(
        title = stringResource(R.string.project_create_artwork),
        description = stringResource(R.string.project_create_artwork_help),
    ) {
        OutlinedButton(onClick = onChooseIcon, modifier = Modifier.fillMaxWidth()) {
            Icon(Lucide.Image, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text(draft.icon?.fileName ?: stringResource(R.string.project_create_add_icon))
        }
        iconError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        draft.icon?.let {
            Text(
                stringResource(R.string.project_create_icon_ready, it.bytes.size / 1024),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun ProjectCompatibilityStep(draft: CreateProjectDraft, metadata: ProjectCreationMetadata) {
    val availableCategories = metadata.categories.filter { it.projectType == draft.projectType }

    FormSection(
        title = stringResource(R.string.project_create_categories),
        description = stringResource(R.string.project_create_categories_help),
    ) {
        if (availableCategories.isEmpty()) {
            Text(stringResource(R.string.project_create_no_categories), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CategorySelector(
                categories = availableCategories,
                selected = draft.categories,
                onToggle = draft::toggleCategory,
            )
        }
    }

    FormSection(
        title = stringResource(R.string.project_create_environment),
        description = stringResource(R.string.project_create_environment_help),
        contentSpacing = 14.dp,
    ) {
        EnvironmentSelector(
            title = stringResource(R.string.project_create_client),
            icon = Lucide.Monitor,
            selected = draft.clientSide,
            onSelect = { draft.clientSide = it },
        )
        EnvironmentSelector(
            title = stringResource(R.string.project_create_server),
            icon = Lucide.Server,
            selected = draft.serverSide,
            onSelect = { draft.serverSide = it },
        )
    }

    FormSection(
        title = stringResource(R.string.project_create_license),
        description = stringResource(R.string.project_create_license_help),
    ) {
        LicenseSelector(
            licenses = metadata.licenses,
            selectedId = draft.licenseId,
            onSelect = { draft.licenseId = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    categories: List<ProjectCategory>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedNames = selected.sorted().take(3).joinToString { it.humanizeIdentifier() }
    SelectionField(
        icon = Lucide.Tag,
        title = stringResource(R.string.project_create_categories),
        value = if (selected.isEmpty()) stringResource(R.string.project_create_not_selected)
            else stringResource(R.string.project_create_categories_selected, selected.size),
        description = when {
            selected.isEmpty() -> stringResource(R.string.project_create_categories_none)
            selected.size > 3 -> stringResource(R.string.project_create_categories_summary_more, selectedNames, selected.size - 3)
            else -> selectedNames
        },
        onClick = { isPickerOpen = true },
    )

    if (isPickerOpen) {
        val matches = categories.filter { it.name.contains(query, ignoreCase = true) }
        val groupedMatches = matches.groupBy(ProjectCategory::header).toSortedMap()
        ModalBottomSheet(
            onDismissRequest = { isPickerOpen = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.project_create_choose_categories), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.project_create_categories_picker_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.project_create_search_categories)) },
                    leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    groupedMatches.forEach { (header, group) ->
                        item(key = "header-$header") {
                            Text(
                                categoryGroupLabel(header),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                        items(group, key = { it.name }) { category ->
                            val isSelected = category.name in selected
                            ListItem(
                                headlineContent = { Text(category.name.humanizeIdentifier()) },
                                supportingContent = { Text(categoryDescription(category), maxLines = 2) },
                                trailingContent = { Checkbox(checked = isSelected, onCheckedChange = null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = isSelected,
                                        role = Role.Checkbox,
                                        onValueChange = { onToggle(category.name) },
                                    ),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicenseSelector(
    licenses: List<ProjectLicense>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = licenses.firstOrNull { it.id.equals(selectedId, ignoreCase = true) }
    val selectedName = selected?.name?.takeIf(String::isNotBlank) ?: selectedId
    SelectionField(
        icon = Lucide.Scale,
        title = stringResource(R.string.project_create_license),
        value = "$selectedName · $selectedId",
        description = licenseDescription(selectedId),
        onClick = { isPickerOpen = true },
    )
    Text(
        stringResource(R.string.project_create_license_legal_note),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (isPickerOpen) {
        val matches = licenses
            .filter { license ->
                query.isBlank() || license.id.contains(query, ignoreCase = true) ||
                    license.name?.contains(query, ignoreCase = true) == true
            }
            .sortedWith(compareByDescending<ProjectLicense> { it.id.equals(selectedId, ignoreCase = true) }.thenBy { it.name ?: it.id })
        val normalizedQuery = query.trim()
        val hasExactMatch = licenses.any { it.id.equals(normalizedQuery, ignoreCase = true) }

        ModalBottomSheet(
            onDismissRequest = { isPickerOpen = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.project_create_choose_license), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.project_create_choose_license_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.project_create_search_licenses)) },
                    leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(matches, key = { it.id }) { license ->
                        val isSelected = license.id.equals(selectedId, ignoreCase = true)
                        ListItem(
                            headlineContent = { Text(license.name ?: license.id, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                            overlineContent = { Text(license.id, color = MaterialTheme.colorScheme.primary) },
                            supportingContent = { Text(licenseDescription(license.id), maxLines = 3) },
                            trailingContent = if (isSelected) {
                                { Icon(Lucide.Check, contentDescription = stringResource(R.string.project_create_selected)) }
                            } else null,
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(license.id)
                                        isPickerOpen = false
                                    },
                                ),
                        )
                        HorizontalDivider()
                    }
                    if (normalizedQuery.isNotEmpty() && !hasExactMatch) {
                        item(key = "custom-license") {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.project_create_use_custom_license, normalizedQuery)) },
                                supportingContent = { Text(stringResource(R.string.project_create_custom_license_help)) },
                                leadingContent = { Icon(Lucide.Scale, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onSelect(normalizedQuery)
                                    isPickerOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProjectPageStep(
    draft: CreateProjectDraft,
    showValidationErrors: Boolean,
) {
    FormSection(
        title = stringResource(R.string.project_create_description),
        description = stringResource(R.string.project_create_description_help),
    ) {
        MarkdownEditor(
            markdown = draft.body,
            mode = draft.editorMode,
            placeholder = stringResource(R.string.project_create_description_hint),
            onMarkdownChange = { draft.body = it.take(ProjectCreationRules.BODY_MAX_LENGTH + 1) },
            onModeChange = { draft.editorMode = it },
            isError = showValidationErrors &&
                (draft.body.isBlank() || draft.body.length > ProjectCreationRules.BODY_MAX_LENGTH),
        )
        if (draft.body.isBlank() || draft.body.length > ProjectCreationRules.BODY_MAX_LENGTH) {
            Text(
                stringResource(
                    if (draft.body.isBlank()) R.string.project_create_description_required
                    else R.string.project_create_description_too_long,
                ),
                color = if (showValidationErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    FormSection(
        title = stringResource(R.string.project_create_links),
        description = stringResource(R.string.project_create_links_help),
        contentSpacing = 12.dp,
    ) {
        ProjectUrlField(stringResource(R.string.project_create_source), draft.sourceUrl, { draft.sourceUrl = it }, Lucide.Link)
        ProjectUrlField(stringResource(R.string.project_create_issues), draft.issuesUrl, { draft.issuesUrl = it }, Lucide.Tag)
        ProjectUrlField(stringResource(R.string.project_create_wiki), draft.wikiUrl, { draft.wikiUrl = it }, Lucide.Globe)
        ProjectUrlField(stringResource(R.string.project_create_discord), draft.discordUrl, { draft.discordUrl = it }, Lucide.Link)
    }

    ProjectDraftNotice()
}

@Composable
private fun ProjectDraftNotice() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                Lucide.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.project_create_private_note),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormSection(
    title: String,
    description: String,
    contentSpacing: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
    ) {
        SectionTitle(title, description)
        content()
    }
}

@Composable
internal fun licenseDescription(id: String): String {
    val normalized = id.uppercase()
    return stringResource(
        when {
            normalized == "MIT" -> R.string.project_create_license_mit
            normalized.startsWith("APACHE-") -> R.string.project_create_license_apache
            normalized.startsWith("AGPL-") -> R.string.project_create_license_agpl
            normalized.startsWith("LGPL-") -> R.string.project_create_license_lgpl
            normalized.startsWith("GPL-") -> R.string.project_create_license_gpl
            normalized.startsWith("MPL-") -> R.string.project_create_license_mpl
            normalized.startsWith("BSD-") || normalized == "0BSD" -> R.string.project_create_license_bsd
            normalized == "CC0-1.0" -> R.string.project_create_license_cc0
            normalized.startsWith("CC-BY") -> R.string.project_create_license_cc_by
            normalized in setOf("ARR", "ALL-RIGHTS-RESERVED") -> R.string.project_create_license_arr
            else -> R.string.project_create_license_other
        },
    )
}

@Composable
private fun projectTypeDescription(type: String): String = stringResource(
    when (type.lowercase()) {
        "mod" -> R.string.project_create_type_mod_help
        "modpack" -> R.string.project_create_type_modpack_help
        "resourcepack" -> R.string.project_create_type_resourcepack_help
        "shader" -> R.string.project_create_type_shader_help
        "plugin" -> R.string.project_create_type_plugin_help
        "datapack" -> R.string.project_create_type_datapack_help
        else -> R.string.project_create_type_other_help
    },
)

@Composable
private fun categoryGroupLabel(header: String): String = stringResource(
    when (header.lowercase()) {
        "features" -> R.string.project_create_category_group_features
        "resolutions" -> R.string.project_create_category_group_resolutions
        "performance impact" -> R.string.project_create_category_group_performance
        else -> R.string.project_create_category_group_categories
    },
)

@Composable
private fun categoryDescription(category: ProjectCategory): String {
    val name = category.name.lowercase()
    val specific = when (name) {
        "adventure" -> R.string.project_create_category_adventure
        "cursed" -> R.string.project_create_category_cursed
        "decoration" -> R.string.project_create_category_decoration
        "economy" -> R.string.project_create_category_economy
        "equipment" -> R.string.project_create_category_equipment
        "food" -> R.string.project_create_category_food
        "game-mechanics" -> R.string.project_create_category_game_mechanics
        "library" -> R.string.project_create_category_library
        "magic" -> R.string.project_create_category_magic
        "management" -> R.string.project_create_category_management
        "minigame" -> R.string.project_create_category_minigame
        "mobs" -> R.string.project_create_category_mobs
        "optimization" -> R.string.project_create_category_optimization
        "social", "multiplayer" -> R.string.project_create_category_social
        "storage" -> R.string.project_create_category_storage
        "technology" -> R.string.project_create_category_technology
        "transportation" -> R.string.project_create_category_transportation
        "utility" -> R.string.project_create_category_utility
        "worldgen" -> R.string.project_create_category_worldgen
        "challenging" -> R.string.project_create_category_challenging
        "combat" -> R.string.project_create_category_combat
        "kitchen-sink" -> R.string.project_create_category_kitchen_sink
        "lightweight" -> R.string.project_create_category_lightweight
        "quests" -> R.string.project_create_category_quests
        "realistic" -> R.string.project_create_category_realistic
        "simplistic" -> R.string.project_create_category_simplistic
        "themed" -> R.string.project_create_category_themed
        "tweaks" -> R.string.project_create_category_tweaks
        "vanilla-like" -> R.string.project_create_category_vanilla_like
        "modded" -> R.string.project_create_category_modded
        else -> null
    }
    if (specific != null) return stringResource(specific)

    return when (category.header.lowercase()) {
        "resolutions" -> stringResource(R.string.project_create_category_resolution, category.name)
        "features" -> stringResource(
            if (category.projectType == "shader") R.string.project_create_category_shader_feature
            else R.string.project_create_category_resource_feature,
            category.name.humanizeIdentifier(),
        )
        "performance impact" -> stringResource(
            when (name) {
                "potato" -> R.string.project_create_category_performance_potato
                "low" -> R.string.project_create_category_performance_low
                "medium" -> R.string.project_create_category_performance_medium
                "high" -> R.string.project_create_category_performance_high
                "screenshot" -> R.string.project_create_category_performance_screenshot
                else -> R.string.project_create_category_other
            },
        )
        else -> stringResource(R.string.project_create_category_other)
    }
}

@Composable
private fun SelectionField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    description: String,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            overlineContent = { Text(title, color = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(value, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(description, maxLines = 3) },
            leadingContent = { Icon(icon, contentDescription = null) },
            trailingContent = { Icon(Lucide.ChevronRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoicePickerSheet(
    title: String,
    description: String,
    values: List<String>,
    selected: String,
    labelFor: @Composable (String) -> String,
    descriptionFor: @Composable (String) -> String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(values, key = { it }) { value ->
                    val isSelected = value == selected
                    ListItem(
                        headlineContent = { Text(labelFor(value), fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        supportingContent = { Text(descriptionFor(value), maxLines = 3) },
                        trailingContent = if (isSelected) {
                            { Icon(Lucide.Check, contentDescription = stringResource(R.string.project_create_selected)) }
                        } else null,
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(value) },
                        ),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EnvironmentSelector(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    SelectionField(
        icon = icon,
        title = title,
        value = environmentLabel(selected),
        description = environmentDescription(selected),
        onClick = { isPickerOpen = true },
    )
    if (isPickerOpen) {
        SingleChoicePickerSheet(
            title = title,
            description = stringResource(R.string.project_create_choose_environment_help),
            values = ProjectCreationRules.environmentValues,
            selected = selected,
            labelFor = { environmentLabel(it) },
            descriptionFor = { environmentDescription(it) },
            onDismiss = { isPickerOpen = false },
            onSelect = {
                onSelect(it)
                isPickerOpen = false
            },
        )
    }
}

@Composable
private fun environmentDescription(value: String): String = stringResource(
    when (value) {
        "required" -> R.string.project_create_environment_required_help
        "optional" -> R.string.project_create_environment_optional_help
        "unsupported" -> R.string.project_create_environment_unsupported_help
        else -> R.string.project_create_environment_unknown_help
    },
)

@Composable
private fun environmentLabel(value: String): String = stringResource(
    when (value) {
        "required" -> R.string.project_create_environment_required
        "optional" -> R.string.project_create_environment_optional
        "unsupported" -> R.string.project_create_environment_unsupported
        else -> R.string.project_create_environment_unknown
    },
)

@Composable
private fun projectTypeLabel(value: String): String = stringResource(
    when (value) {
        "mod" -> R.string.project_create_type_mod
        "modpack" -> R.string.project_create_type_modpack
        "resourcepack" -> R.string.project_create_type_resourcepack
        "shader" -> R.string.project_create_type_shader
        "plugin" -> R.string.project_create_type_plugin
        "datapack" -> R.string.project_create_type_datapack
        else -> R.string.project_create_type_other
    },
)

private fun String.humanizeIdentifier(): String =
    replace('_', ' ').replace('-', ' ').replaceFirstChar(Char::uppercase)

@Composable
private fun ProjectUrlField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val isError = value.isNotBlank() && !value.startsWith("https://") && !value.startsWith("http://")
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("https://…") },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = {
            Text(if (isError) stringResource(R.string.project_create_url_error) else stringResource(R.string.project_create_optional))
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}
