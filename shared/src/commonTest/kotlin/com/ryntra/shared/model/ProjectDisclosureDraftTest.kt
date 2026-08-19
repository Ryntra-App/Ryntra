package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectDisclosureDraftTest {
    private val mod = Project(id = "AAAAAAAA", title = "Sample", projectType = "mod", loaders = listOf("fabric"))
    private val resourcePack = Project(id = "BBBBBBBB", title = "Pack", projectType = "resourcepack")

    @Test
    fun draftOffersEveryDisclosureModrinthSupportsForAMod() {
        val draft = ProjectDisclosureDraft.from(mod, emptyList())

        assertEquals(DisclosureRules.orderedTypes, draft.entries.map { it.type })
        assertTrue(draft.entries.none { it.enabled })
    }

    @Test
    fun resourcePacksCannotDeclareTelemetryOrSystemInteractions() {
        val types = ProjectDisclosureDraft.from(resourcePack, emptyList()).entries.map { it.type }

        assertFalse(DisclosureType.Telemetry in types)
        assertFalse(DisclosureType.SystemInteractions in types)
        assertContains(types, DisclosureType.AiContent)
    }

    @Test
    fun pluginsKeepTelemetryEvenThoughModrinthReportsThemAsMods() {
        val plugin = Project(id = "CCCCCCCC", title = "Plugin", projectType = "mod", loaders = listOf("paper"))

        val types = ProjectDisclosureDraft.from(plugin, emptyList()).entries.map { it.type }

        assertContains(types, DisclosureType.Telemetry)
        assertContains(types, DisclosureType.SystemInteractions)
    }

    @Test
    fun softDeletedDisclosuresComeBackDisabledButKeepTheirDetails() {
        val stored = ProjectDisclosure(
            type = DisclosureType.AiContent,
            enabled = false,
            note = "Translations only",
            uses = listOf(AiUsage.Text),
        )

        val entry = ProjectDisclosureDraft.from(mod, listOf(stored)).entryOf(DisclosureType.AiContent)!!

        assertFalse(entry.enabled)
        assertEquals("Translations only", entry.note)
    }

    @Test
    fun enablingAListBackedDisclosureSeedsOneRow() {
        val telemetry = ProjectDisclosure(type = DisclosureType.Telemetry).withEnabled(true)

        assertEquals(listOf(""), telemetry.dataCollected)
    }

    @Test
    fun anEnabledDisclosureMissingItsRequiredDetailBlocksSaving() {
        val draft = ProjectDisclosureDraft.from(mod, emptyList())
            .replacing(ProjectDisclosure(type = DisclosureType.Advertisements).withEnabled(true))

        assertEquals(listOf(DisclosureIssue.AdvertisingNote), draft.issues())
        assertFalse(draft.canSave)
    }

    @Test
    fun aiDisclosureNeedsNoNote() {
        val draft = ProjectDisclosureDraft.from(mod, emptyList())
            .replacing(ProjectDisclosure(type = DisclosureType.AiContent).withEnabled(true))

        assertTrue(draft.canSave)
    }

    @Test
    fun derivativeSourcesMustCarryAName() {
        val sources = listOf(DerivativeSource(link = "https://example.com"))
        val draft = ProjectDisclosureDraft.from(mod, emptyList())
            .replacing(
                ProjectDisclosure(type = DisclosureType.DerivativeWork)
                    .withEnabled(true)
                    .withSources(sources),
            )

        assertEquals(listOf(DisclosureIssue.DerivativeSourceLabel), draft.issues())
    }

    @Test
    fun onlyTouchedDisclosuresAreSent() {
        val baseline = ProjectDisclosureDraft.from(
            mod,
            listOf(
                ProjectDisclosure(type = DisclosureType.AiContent, enabled = true, uses = listOf(AiUsage.Code)),
                ProjectDisclosure(type = DisclosureType.Advertisements, enabled = true, note = "Sponsor link"),
            ),
        )
        val draft = baseline
            .replacing(baseline.entryOf(DisclosureType.Advertisements)!!.withEnabled(false))
            .replacing(baseline.entryOf(DisclosureType.PaidFeatures)!!.withEnabled(true).withFeatures(listOf("Cosmetics")))

        val changes = draft.changesFrom(baseline)

        assertEquals(listOf(DisclosureType.PaidFeatures), changes.set.map { it.type })
        assertEquals(listOf(DisclosureType.Advertisements), changes.remove)
    }

    @Test
    fun whitespaceOnlyEditsAreNotChanges() {
        val baseline = ProjectDisclosureDraft.from(
            mod,
            listOf(ProjectDisclosure(type = DisclosureType.Archived, enabled = true, note = "Unmaintained")),
        )
        val draft = baseline.replacing(baseline.entryOf(DisclosureType.Archived)!!.withNote("  Unmaintained  "))

        assertTrue(draft.changesFrom(baseline).isEmpty)
        assertFalse(draft.hasChangesFrom(baseline))
    }

    @Test
    fun moderatorLockedDisclosuresReportWhatTheCreatorMayStillDo() {
        val locked = ProjectDisclosure(
            type = DisclosureType.AiContent,
            enabled = true,
            lockStatus = DisclosureLockStatus.CannotDisable,
        )

        assertFalse(locked.canDisable)
        assertTrue(locked.canEdit)
    }
}
