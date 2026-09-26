package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.PayoutStatus
import com.ryntra.shared.model.WalletTransaction
import com.ryntra.shared.model.WalletTransactionKind
import com.ryntra.shared.network.PayoutBalanceResponse
import com.ryntra.shared.network.PayoutHistoryResponse
import com.ryntra.shared.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * The creator wallet on labrinth's **v3** payout routes; the client's base URL is v2, so the
 * routes are absolute. Money arrives as `rust_decimal` values, which serialize as strings.
 *
 * Balance and history report a status instead of throwing so the wallet can still show
 * whichever half loaded.
 */
internal class PayoutEndpoints(
    private val client: HttpClient,
) {
    suspend fun getBalance(token: String): PayoutBalanceResponse = requestJson(
        call = { client.get("$PAYOUT_BASE/balance") { authorize(token) } },
        onFailure = ::PayoutBalanceResponse,
    ) { status, root ->
        val balance = root as? JsonObject
        PayoutBalanceResponse(
            status = status,
            available = balance.decimal("available"),
            pending = balance.decimal("pending"),
            withdrawnLifetime = balance.decimal("withdrawn_lifetime"),
            withdrawnThisYear = balance.decimal("withdrawn_ytd"),
            dates = (balance?.get("dates") as? JsonObject).orEmpty()
                .mapNotNull { (date, amount) -> amount.decimalOrNull()?.let { date to it } }
                .toMap(),
            formCompletionStatus = balance?.get("form_completion_status").stringOrNull(),
        )
    }

    suspend fun getHistory(token: String): PayoutHistoryResponse = requestJson(
        call = { client.get("$PAYOUT_BASE/history") { authorize(token) } },
        onFailure = ::PayoutHistoryResponse,
    ) { status, root ->
        PayoutHistoryResponse(
            status = status,
            transactions = (root as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.toTransaction() }
                .sortedByDescending(WalletTransaction::created),
        )
    }

    /** `DELETE /v3/payout/{id}`; Modrinth only accepts it while the withdrawal is in transit. */
    suspend fun cancel(payoutId: String, token: String) {
        client.delete("$PAYOUT_BASE/$payoutId") { authorize(token) }.ensureSuccess()
    }

    /**
     * Yearly withdrawal limits before a tax form is required, from the public globals route the
     * site itself is built from. Keys are years, values US dollars.
     */
    suspend fun getTaxFormThresholds(): Map<Int, Double> {
        cachedTaxFormThresholds?.let { return it }
        return try {
            // Only the tax notice depends on this, so it must never hold the balance back
            // for long; the table changes once a year and is kept once it has loaded.
            val response = client.get(GLOBALS_URL) {
                timeout { requestTimeoutMillis = GLOBALS_TIMEOUT_MILLIS }
            }
            if (!response.status.isSuccess()) {
                emptyMap()
            } else {
                val root = apiJson.parseToJsonElement(response.bodyAsText()) as? JsonObject
                (root?.get("tax_compliance_thresholds") as? JsonObject).orEmpty()
                    .mapNotNull { (year, amount) ->
                        val parsedYear = year.toIntOrNull() ?: return@mapNotNull null
                        amount.decimalOrNull()?.let { parsedYear to it }
                    }
                    .toMap()
                    .also { if (it.isNotEmpty()) cachedTaxFormThresholds = it }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private var cachedTaxFormThresholds: Map<Int, Double>? = null

    private suspend fun <T> requestJson(
        call: suspend () -> HttpResponse,
        onFailure: (Int) -> T,
        parse: (Int, JsonElement) -> T,
    ): T = try {
        val response = call()
        if (!response.status.isSuccess()) {
            onFailure(response.status.value)
        } else {
            parse(response.status.value, apiJson.parseToJsonElement(response.bodyAsText()))
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        onFailure(0)
    }

    private companion object {
        const val PAYOUT_BASE = "https://api.modrinth.com/v3/payout"
        const val GLOBALS_URL = "https://api.modrinth.com/_internal/globals"
        const val GLOBALS_TIMEOUT_MILLIS = 5_000L
    }
}

private fun JsonObject.toTransaction(): WalletTransaction? {
    val created = get("created").stringOrNull() ?: return null
    val amount = get("amount")?.decimalOrNull() ?: return null
    return when (get("type").stringOrNull()) {
        "withdrawal" -> WalletTransaction(
            kind = WalletTransactionKind.Withdrawal,
            created = created,
            amount = amount,
            id = get("id").stringOrNull(),
            status = PayoutStatus.fromApi(get("status").stringOrNull()),
            fee = get("fee")?.decimalOrNull(),
            methodType = get("method_type").stringOrNull() ?: get("method").stringOrNull(),
            methodAddress = get("method_address").stringOrNull(),
        )
        "payout_available" -> WalletTransaction(
            kind = WalletTransactionKind.Income,
            created = created,
            amount = amount,
            payoutSource = get("payout_source").stringOrNull(),
        )
        else -> null
    }
}

private fun JsonObject?.decimal(key: String): Double? = this?.get(key)?.decimalOrNull()

private fun JsonElement.decimalOrNull(): Double? =
    (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.toDoubleOrNull()

private fun JsonElement?.stringOrNull(): String? =
    (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
