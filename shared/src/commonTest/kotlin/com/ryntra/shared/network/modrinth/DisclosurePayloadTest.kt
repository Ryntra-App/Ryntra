package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.AiUsage
import com.ryntra.shared.model.DerivativeSource
import com.ryntra.shared.model.DisclosureChangeSet
import com.ryntra.shared.model.DisclosureType
import com.ryntra.shared.model.ProjectDisclosure
import com.ryntra.shared.model.TelemetryConsent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisclosurePayloadTest {
    @Test
    fun bothListsAreAlwaysPresentSoLabrinthCanDeserializeTheRequest() {
        val payload = encodeDisclosureChanges(
            DisclosureChangeSet(remove = listOf(DisclosureType.Advertisements)),
        )

        assertEquals(JsonArray(emptyList()), payload["set"])
        assertEquals(JsonArray(listOf(JsonPrimitive("advertisements"))), payload["remove"])
    }

    @Test
    fun aiDisclosureSendsItsUsesEvenWhenNoneAreSelected() {
        val payload = firstSet(ProjectDisclosure(type = DisclosureType.AiContent, enabled = true))

        assertEquals(JsonPrimitive("ai_content"), payload["type"])
        assertEquals(JsonArray(emptyList()), payload["uses"])
        assertEquals(JsonNull, payload["note"])
    }

    @Test
    fun aiUsesKeepModrinthsOwnOrderRegardlessOfHowTheyWereTicked() {
        val payload = firstSet(
            ProjectDisclosure(
                type = DisclosureType.AiContent,
                enabled = true,
                uses = listOf(AiUsage.Text, AiUsage.Code),
            ),
        )

        assertEquals(listOf("code", "text"), payload["uses"]!!.jsonArray.map { it.toStringValue() })
    }

    @Test
    fun telemetrySendsConsentAndTrimmedDataEntries() {
        val payload = firstSet(
            ProjectDisclosure(
                type = DisclosureType.Telemetry,
                enabled = true,
                consent = TelemetryConsent.AlwaysActive,
                dataCollected = listOf("  Loader usage  ", "   ", "Mod list"),
            ),
        )

        assertEquals(JsonPrimitive("always_active"), payload["consent"])
        assertEquals(
            listOf("Loader usage", "Mod list"),
            payload["data_collected"]!!.jsonArray.map { it.toStringValue() },
        )
    }

    @Test
    fun derivativeSourcesDropBlankRowsAndNullTheirOptionalFields() {
        val payload = firstSet(
            ProjectDisclosure(
                type = DisclosureType.DerivativeWork,
                enabled = true,
                sources = listOf(
                    DerivativeSource(label = "Example project", link = " https://example.com "),
                    DerivativeSource(),
                ),
            ),
        )

        val sources = payload["sources"]!!.jsonArray
        assertEquals(1, sources.size)
        assertEquals(JsonPrimitive("Example project"), sources[0].jsonObject["label"])
        assertEquals(JsonPrimitive("https://example.com"), sources[0].jsonObject["link"])
        assertEquals(JsonNull, sources[0].jsonObject["note"])
    }

    @Test
    fun paidFeaturesSendOnlyTheFieldsThatBelongToTheVariant() {
        val payload = firstSet(
            ProjectDisclosure(
                type = DisclosureType.PaidFeatures,
                enabled = true,
                features = listOf("Patreon cosmetics"),
                note = "ignored for this type",
            ),
        )

        assertEquals(setOf("type", "features"), payload.keys)
        assertTrue("note" !in payload)
    }

    private fun firstSet(disclosure: ProjectDisclosure): JsonObject =
        encodeDisclosureChanges(DisclosureChangeSet(set = listOf(disclosure)))["set"]!!
            .jsonArray
            .first()
            .jsonObject

    private fun kotlinx.serialization.json.JsonElement.toStringValue(): String =
        (this as JsonPrimitive).content
}
