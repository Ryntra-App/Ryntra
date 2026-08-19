package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.AiUsage
import com.ryntra.shared.model.DerivativeSource
import com.ryntra.shared.model.DisclosureChangeSet
import com.ryntra.shared.model.DisclosureLockStatus
import com.ryntra.shared.model.DisclosureType
import com.ryntra.shared.model.ProjectDisclosure
import com.ryntra.shared.model.TelemetryConsent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Content disclosures live on **v3 only**; the client's base URL is v2, so both routes are
 * absolute. See GET / PATCH `/v3/project/{id}/disclosures`.
 */
internal class DisclosureEndpoints(
    private val client: HttpClient,
) {
    suspend fun getForProject(projectIdOrSlug: String, token: String): List<ProjectDisclosure> {
        val response: DisclosuresResponse = client.get(
            "https://api.modrinth.com/v3/project/$projectIdOrSlug/disclosures",
        ) { authorize(token) }.decode()
        return response.disclosures.mapNotNull(DisclosureDto::toModel)
    }

    suspend fun modify(projectIdOrSlug: String, changes: DisclosureChangeSet, token: String) {
        if (changes.isEmpty) return
        client.patch("https://api.modrinth.com/v3/project/$projectIdOrSlug/disclosures") {
            authorize(token)
            contentType(ContentType.Application.Json)
            setBody(encodeDisclosureChanges(changes))
        }.ensureSuccess()
    }
}

/**
 * labrinth deserializes each disclosure into a tagged Rust enum whose per-variant collections are
 * not optional, while `apiJson` omits default values. The payload is therefore assembled by hand
 * so every field the variant requires is always present.
 */
internal fun encodeDisclosureChanges(changes: DisclosureChangeSet): JsonObject = JsonObject(
    mapOf(
        "set" to JsonArray(changes.set.map(::encodeDisclosure)),
        "remove" to JsonArray(changes.remove.map { JsonPrimitive(it.apiValue) }),
    ),
)

private fun encodeDisclosure(disclosure: ProjectDisclosure): JsonObject {
    val payload = disclosure.normalized()
    val fields = mutableMapOf<String, JsonElement>(
        "type" to JsonPrimitive(payload.type.apiValue),
    )
    when (payload.type) {
        DisclosureType.AiContent -> {
            fields["note"] = payload.note.orJsonNull()
            fields["uses"] = JsonArray(payload.uses.map { JsonPrimitive(it.apiValue) })
        }
        DisclosureType.Advertisements,
        DisclosureType.EpilepsyTriggers,
        DisclosureType.Archived,
        -> fields["note"] = payload.note.orJsonNull()
        DisclosureType.SystemInteractions -> {
            fields["note"] = payload.note.orJsonNull()
            fields["interactions"] = JsonArray(payload.interactions.map(::JsonPrimitive))
        }
        DisclosureType.Telemetry -> {
            fields["consent"] = JsonPrimitive(payload.consent.apiValue)
            fields["data_collected"] = JsonArray(payload.dataCollected.map(::JsonPrimitive))
        }
        DisclosureType.DerivativeWork -> fields["sources"] = JsonArray(
            payload.sources.map { source ->
                JsonObject(
                    mapOf(
                        "label" to JsonPrimitive(source.label),
                        "link" to source.link.orJsonNull(),
                        "note" to source.note.orJsonNull(),
                    ),
                )
            },
        )
        DisclosureType.PaidFeatures -> fields["features"] = JsonArray(payload.features.map(::JsonPrimitive))
    }
    return JsonObject(fields)
}

private fun String.orJsonNull(): JsonElement =
    if (isBlank()) JsonNull else JsonPrimitive(this)

@Serializable
private data class DisclosuresResponse(
    val disclosures: List<DisclosureDto> = emptyList(),
)

@Serializable
private data class DisclosureDto(
    val type: String,
    val note: String? = null,
    val uses: List<String> = emptyList(),
    val interactions: List<String> = emptyList(),
    val consent: String? = null,
    @SerialName("data_collected") val dataCollected: List<String> = emptyList(),
    val sources: List<DerivativeSourceDto> = emptyList(),
    val features: List<String> = emptyList(),
    @SerialName("set_by_moderator") val setByModerator: Boolean = false,
    @SerialName("lock_status") val lockStatus: String = "unlocked",
    @SerialName("updated_at") val updatedAt: String? = null,
    /** Present only on disclosures Modrinth withdrew; they stay visible to the team as disabled. */
    @SerialName("deleted_at") val deletedAt: String? = null,
) {
    fun toModel(): ProjectDisclosure? {
        val resolvedType = DisclosureType.fromApi(type) ?: return null
        return ProjectDisclosure(
            type = resolvedType,
            enabled = deletedAt == null,
            note = note.orEmpty(),
            uses = uses.mapNotNull(AiUsage::fromApi),
            interactions = interactions,
            consent = consent?.let(TelemetryConsent::fromApi) ?: TelemetryConsent.OptIn,
            dataCollected = dataCollected,
            sources = sources.map(DerivativeSourceDto::toModel),
            features = features,
            lockStatus = DisclosureLockStatus.fromApi(lockStatus),
            setByModerator = setByModerator,
            updatedAt = updatedAt,
        )
    }
}

@Serializable
private data class DerivativeSourceDto(
    val label: String = "",
    val link: String? = null,
    val note: String? = null,
) {
    fun toModel(): DerivativeSource = DerivativeSource(
        label = label,
        link = link.orEmpty(),
        note = note.orEmpty(),
    )
}
