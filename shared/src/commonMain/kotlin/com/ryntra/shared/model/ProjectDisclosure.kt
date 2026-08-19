package com.ryntra.shared.model

/**
 * Content disclosures a creator must declare on a Modrinth project.
 * See GET / PATCH `/v3/project/{id}/disclosures` and Section 6 of Modrinth's Content Rules.
 */
enum class DisclosureType(val apiValue: String) {
    AiContent("ai_content"),
    Advertisements("advertisements"),
    EpilepsyTriggers("epilepsy_triggers"),
    SystemInteractions("system_interactions"),
    Telemetry("telemetry"),
    DerivativeWork("derivative_work"),
    PaidFeatures("paid_features"),
    Archived("archived"),
    ;

    companion object {
        fun fromApi(value: String): DisclosureType? = entries.firstOrNull { it.apiValue == value }
    }
}

/** What generative AI was used for, on the `ai_content` disclosure. */
enum class AiUsage(val apiValue: String) {
    Code("code"),
    Assets("assets"),
    Text("text"),
    Functionality("functionality"),
    ;

    companion object {
        fun fromApi(value: String): AiUsage? = entries.firstOrNull { it.apiValue == value }
    }
}

/** Consent model of the telemetry a project collects. */
enum class TelemetryConsent(val apiValue: String) {
    OptIn("opt_in"),
    OptOut("opt_out"),
    AlwaysActive("always_active"),
    ;

    companion object {
        fun fromApi(value: String): TelemetryConsent = entries.firstOrNull { it.apiValue == value } ?: OptIn
    }
}

/**
 * Set by moderators when a disclosure must stay in place.
 * `CannotDisable` still allows editing the details; `FullyLocked` allows neither.
 */
enum class DisclosureLockStatus(val apiValue: String) {
    Unlocked("unlocked"),
    CannotDisable("cannot_disable"),
    FullyLocked("fully_locked"),
    ;

    val allowsEdit: Boolean get() = this != FullyLocked
    val allowsRemoval: Boolean get() = this == Unlocked

    companion object {
        fun fromApi(value: String): DisclosureLockStatus =
            entries.firstOrNull { it.apiValue == value } ?: Unlocked
    }
}

/** One original work a derivative project is based on. */
data class DerivativeSource(
    val label: String = "",
    val link: String = "",
    val note: String = "",
) {
    fun withLabel(label: String): DerivativeSource = copy(label = label)

    fun withLink(link: String): DerivativeSource = copy(link = link)

    fun withNote(note: String): DerivativeSource = copy(note = note)

    internal fun normalized(): DerivativeSource = DerivativeSource(
        label = label.trim(),
        link = link.trim(),
        note = note.trim(),
    )

    internal val isBlank: Boolean get() = label.isBlank() && link.isBlank() && note.isBlank()
}

/**
 * A single disclosure, both as returned by Modrinth and as edited locally.
 *
 * Only the fields belonging to [type] carry meaning — the API models each disclosure as a
 * tagged union, but a flat shape keeps the Swift and Compose editors symmetrical and matches
 * how [MarkdownBlock] is already modelled here.
 *
 * The `with*` helpers exist because Swift cannot call Kotlin's generated `copy` with named
 * defaults; every editor mutation goes through one of them.
 */
data class ProjectDisclosure(
    val type: DisclosureType,
    /** False for a disclosure the creator never set, or one Modrinth soft-deleted. */
    val enabled: Boolean = false,
    val note: String = "",
    val uses: List<AiUsage> = emptyList(),
    /** Free-form list the web UI does not edit; preserved so a save never drops it. */
    val interactions: List<String> = emptyList(),
    val consent: TelemetryConsent = TelemetryConsent.OptIn,
    val dataCollected: List<String> = emptyList(),
    val sources: List<DerivativeSource> = emptyList(),
    val features: List<String> = emptyList(),
    val lockStatus: DisclosureLockStatus = DisclosureLockStatus.Unlocked,
    val setByModerator: Boolean = false,
    /** ISO timestamp of the last change, or null for a disclosure that was never set. */
    val updatedAt: String? = null,
) {
    val canDisable: Boolean get() = lockStatus.allowsRemoval
    val canEdit: Boolean get() = lockStatus.allowsEdit

    /**
     * Enabling a list-backed disclosure seeds one blank row so the editor never opens empty,
     * which is what the Modrinth web form does.
     */
    fun withEnabled(enabled: Boolean): ProjectDisclosure {
        if (enabled == this.enabled) return this
        if (!enabled) return copy(enabled = false)
        return copy(enabled = true).seeded()
    }

    fun withNote(note: String): ProjectDisclosure = copy(note = note)

    fun withUse(use: AiUsage, selected: Boolean): ProjectDisclosure = copy(
        uses = if (selected) (uses + use).distinct() else uses.filterNot { it == use },
    )

    fun hasUse(use: AiUsage): Boolean = use in uses

    fun withConsent(consent: TelemetryConsent): ProjectDisclosure = copy(consent = consent)

    fun withDataCollected(dataCollected: List<String>): ProjectDisclosure = copy(dataCollected = dataCollected)

    fun withFeatures(features: List<String>): ProjectDisclosure = copy(features = features)

    fun withSources(sources: List<DerivativeSource>): ProjectDisclosure = copy(sources = sources)

    /** The rows the editor renders for a list-backed disclosure, never empty while enabled. */
    fun editableRows(): List<String> = when (type) {
        DisclosureType.Telemetry -> dataCollected.ifEmpty { listOf("") }
        DisclosureType.PaidFeatures -> features.ifEmpty { listOf("") }
        else -> emptyList()
    }

    fun editableSources(): List<DerivativeSource> = sources.ifEmpty { listOf(DerivativeSource()) }

    private fun seeded(): ProjectDisclosure = when (type) {
        DisclosureType.Telemetry -> if (dataCollected.isEmpty()) copy(dataCollected = listOf("")) else this
        DisclosureType.PaidFeatures -> if (features.isEmpty()) copy(features = listOf("")) else this
        DisclosureType.DerivativeWork -> if (sources.isEmpty()) copy(sources = listOf(DerivativeSource())) else this
        else -> this
    }

    /**
     * Payload as Modrinth stores it: trimmed, blanks dropped, and fields that do not belong to
     * [type] cleared. Two disclosures that normalize equal need no PATCH.
     */
    internal fun normalized(): ProjectDisclosure = when (type) {
        DisclosureType.AiContent -> ProjectDisclosure(
            type = type,
            enabled = enabled,
            note = note.trim(),
            uses = AiUsage.entries.filter { it in uses },
        )
        DisclosureType.Advertisements,
        DisclosureType.EpilepsyTriggers,
        DisclosureType.Archived,
        -> ProjectDisclosure(type = type, enabled = enabled, note = note.trim())
        DisclosureType.SystemInteractions -> ProjectDisclosure(
            type = type,
            enabled = enabled,
            note = note.trim(),
            interactions = interactions.map(String::trim).filter(String::isNotEmpty),
        )
        DisclosureType.Telemetry -> ProjectDisclosure(
            type = type,
            enabled = enabled,
            consent = consent,
            dataCollected = dataCollected.map(String::trim).filter(String::isNotEmpty),
        )
        DisclosureType.DerivativeWork -> ProjectDisclosure(
            type = type,
            enabled = enabled,
            sources = sources.filterNot { it.isBlank }.map { it.normalized() },
        )
        DisclosureType.PaidFeatures -> ProjectDisclosure(
            type = type,
            enabled = enabled,
            features = features.map(String::trim).filter(String::isNotEmpty),
        )
    }
}

/**
 * A rule the creator must satisfy before a disclosure can be saved.
 *
 * [key] exists for the Swift layer: Kotlin/Native rewrites enum entry names on export, so the
 * Apple UI matches on this stable string rather than on a bridged case name.
 */
enum class DisclosureIssue(val key: String) {
    AdvertisingNote("advertising_note"),
    PhotosensitivityNote("photosensitivity_note"),
    SystemInteractionsNote("system_interactions_note"),
    TelemetryEmpty("telemetry_empty"),
    DerivativeEmpty("derivative_empty"),
    DerivativeSourceLabel("derivative_source_label"),
    PaidFeaturesEmpty("paid_features_empty"),
}

/** What a save has to send: disclosures to upsert, and disclosure types to withdraw. */
data class DisclosureChangeSet(
    val set: List<ProjectDisclosure> = emptyList(),
    val remove: List<DisclosureType> = emptyList(),
) {
    val isEmpty: Boolean get() = set.isEmpty() && remove.isEmpty()
}

/**
 * The disclosures editor's whole state: one entry per disclosure that applies to the project,
 * in the order Modrinth lists them.
 */
data class ProjectDisclosureDraft(
    val entries: List<ProjectDisclosure> = emptyList(),
) {
    fun entryOf(type: DisclosureType): ProjectDisclosure? = entries.firstOrNull { it.type == type }

    fun replacing(entry: ProjectDisclosure): ProjectDisclosureDraft = copy(
        entries = entries.map { if (it.type == entry.type) entry else it },
    )

    fun issues(): List<DisclosureIssue> = entries.filter { it.enabled }.mapNotNull { entry ->
        val normalized = entry.normalized()
        when (entry.type) {
            DisclosureType.Advertisements ->
                DisclosureIssue.AdvertisingNote.takeIf { normalized.note.isEmpty() }
            DisclosureType.EpilepsyTriggers ->
                DisclosureIssue.PhotosensitivityNote.takeIf { normalized.note.isEmpty() }
            DisclosureType.SystemInteractions ->
                DisclosureIssue.SystemInteractionsNote.takeIf { normalized.note.isEmpty() }
            DisclosureType.Telemetry ->
                DisclosureIssue.TelemetryEmpty.takeIf { normalized.dataCollected.isEmpty() }
            DisclosureType.DerivativeWork -> when {
                normalized.sources.isEmpty() -> DisclosureIssue.DerivativeEmpty
                normalized.sources.any { it.label.isEmpty() } -> DisclosureIssue.DerivativeSourceLabel
                else -> null
            }
            DisclosureType.PaidFeatures ->
                DisclosureIssue.PaidFeaturesEmpty.takeIf { normalized.features.isEmpty() }
            DisclosureType.AiContent, DisclosureType.Archived -> null
        }
    }

    val canSave: Boolean get() = issues().isEmpty()

    fun changesFrom(baseline: ProjectDisclosureDraft): DisclosureChangeSet {
        val set = mutableListOf<ProjectDisclosure>()
        val remove = mutableListOf<DisclosureType>()
        entries.forEach { entry ->
            val previous = baseline.entryOf(entry.type)
            val next = entry.normalized()
            val before = previous?.normalized()
            when {
                next.enabled && next != before -> set += next
                !next.enabled && before?.enabled == true -> remove += entry.type
            }
        }
        return DisclosureChangeSet(set = set, remove = remove)
    }

    fun hasChangesFrom(baseline: ProjectDisclosureDraft): Boolean = !changesFrom(baseline).isEmpty

    companion object {
        /**
         * Builds the editor state for [project] from the disclosures Modrinth returned, adding a
         * disabled placeholder for every disclosure the project's type supports but has not set.
         */
        fun from(project: Project, disclosures: List<ProjectDisclosure>): ProjectDisclosureDraft =
            ProjectDisclosureDraft(
                entries = DisclosureRules.supportedTypes(project).map { type ->
                    disclosures.firstOrNull { it.type == type } ?: ProjectDisclosure(type = type)
                },
            )
    }
}

/** Which disclosures apply to which kinds of project, mirroring Modrinth's own matrix. */
object DisclosureRules {
    /** Modrinth renders the disclosures in this order; the apps follow it. */
    val orderedTypes: List<DisclosureType> = listOf(
        DisclosureType.AiContent,
        DisclosureType.Advertisements,
        DisclosureType.EpilepsyTriggers,
        DisclosureType.SystemInteractions,
        DisclosureType.Telemetry,
        DisclosureType.DerivativeWork,
        DisclosureType.PaidFeatures,
        DisclosureType.Archived,
    )

    /** Swift cannot reach a Kotlin enum's `entries`, so the pickers read these instead. */
    val aiUsages: List<AiUsage> = AiUsage.entries.toList()
    val telemetryConsents: List<TelemetryConsent> = TelemetryConsent.entries.toList()

    private const val MOD = "mod"
    private const val PLUGIN = "plugin"
    private const val MODPACK = "modpack"
    private const val SERVER = "server"

    private val telemetryTypes = setOf(MOD, PLUGIN, MODPACK, SERVER)
    private val systemInteractionTypes = setOf(MOD, PLUGIN, MODPACK)

    fun supportedTypes(project: Project): List<DisclosureType> {
        val kinds = projectKinds(project)
        return orderedTypes.filter { type -> supports(type, kinds) }
    }

    fun supports(type: DisclosureType, project: Project): Boolean = supports(type, projectKinds(project))

    private fun supports(type: DisclosureType, kinds: Set<String>): Boolean = when (type) {
        DisclosureType.Telemetry -> kinds.isEmpty() || kinds.any { it in telemetryTypes }
        DisclosureType.SystemInteractions -> kinds.isEmpty() || kinds.any { it in systemInteractionTypes }
        else -> true
    }

    /**
     * v2 reports plugins as `project_type = "mod"`, so the loader-aware display kind decides
     * instead. An unrecognised project yields an empty set, which keeps every disclosure offered
     * rather than silently hiding one the creator may be required to declare.
     */
    private fun projectKinds(project: Project): Set<String> = when (project.displayKind()) {
        ProjectDisplayKind.Mod -> setOf(MOD)
        ProjectDisplayKind.Plugin -> setOf(PLUGIN)
        ProjectDisplayKind.Hybrid -> setOf(MOD, PLUGIN)
        ProjectDisplayKind.Modpack -> setOf(MODPACK)
        ProjectDisplayKind.ResourcePack -> setOf("resourcepack")
        ProjectDisplayKind.Shader -> setOf("shader")
        ProjectDisplayKind.DataPack -> setOf("datapack")
        ProjectDisplayKind.Server -> setOf(SERVER)
        ProjectDisplayKind.Project -> emptySet()
    }
}
