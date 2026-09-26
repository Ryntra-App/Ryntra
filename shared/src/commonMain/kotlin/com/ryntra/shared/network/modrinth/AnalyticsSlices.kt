package com.ryntra.shared.network.modrinth

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The time slices of a `POST /v3/analytics` response. labrinth answers with an object —
 * `{"metrics": [slice, …], "projects": {…}, …}` — where every slice is a list of entries.
 */
internal fun analyticsSlices(root: JsonElement): List<List<JsonObject>> {
    val slices = when (root) {
        is JsonObject -> root["metrics"] as? JsonArray
        is JsonArray -> root
        else -> null
    }.orEmpty()
    return slices.map { slice -> (slice as? JsonArray).orEmpty().mapNotNull { it as? JsonObject } }
}
