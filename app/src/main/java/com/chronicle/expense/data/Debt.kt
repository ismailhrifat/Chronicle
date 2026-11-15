package com.chronicle.expense.data

import com.google.firebase.firestore.DocumentSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

const val DEBT_LENT = "lent"
const val DEBT_BORROWED = "borrowed"
const val ACTION_INITIAL = "initial"
const val ACTION_INCREASE = "increase"
const val ACTION_REPAY = "repay"

/** One entry in a person's debt history. Mirrors the Debt app's record shape. */
data class DebtRecord(
    val action: String = ACTION_INITIAL,
    /** Minor units. */
    val amount: Long = 0,
    val description: String = "",
    /** Account name as shown; for imported records this is the Debt app's label. */
    val account: String = "",
    /** `yyyy-MM-dd` */
    val date: String = "",
    val timestamp: String = "",
    /** Chronicle account this record moved money on; null for imported or "Other" records. */
    val accountId: String? = null,
    /** History entry that carries the balance change. */
    val txId: String? = null,
    /** True when money left my account. Null on records imported from the Debt app. */
    val out: Boolean? = null,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "action" to action, "amount" to amount, "description" to description, "account" to account,
        "date" to date, "timestamp" to timestamp, "accountId" to accountId, "txId" to txId, "out" to out,
    )

    companion object {
        fun from(map: Map<*, *>, legacy: Boolean = false) = DebtRecord(
            action = map["action"] as? String ?: ACTION_INITIAL,
            amount = if (legacy) legacyAmount(map["amount"]) else (map["amount"] as? Number)?.toLong() ?: 0,
            description = map["description"] as? String ?: "",
            account = map["account"] as? String ?: "",
            date = map["date"] as? String ?: "",
            timestamp = map["timestamp"] as? String ?: "",
            accountId = map["accountId"] as? String,
            txId = map["txId"] as? String,
            out = map["out"] as? Boolean,
        )
    }
}

/** Everything lent to or borrowed from one person. */
data class Debt(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = DEBT_LENT,
    /** Outstanding amount in minor units, never negative. */
    val amount: Long = 0,
    val originalAmount: Long = 0,
    val status: String = "active",
    /** ISO-8601 instant. */
    val lastUpdated: String = "",
    val records: List<DebtRecord> = emptyList(),
    val archived: Boolean = false,
) {
    val isLent get() = type == DEBT_LENT
    val isClosed get() = amount == 0L

    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name, "description" to description, "type" to type, "amount" to amount,
        "originalAmount" to originalAmount, "status" to status, "lastUpdated" to lastUpdated,
        "records" to records.map { it.toMap() }, "archived" to archived,
    )

    companion object {
        fun from(doc: DocumentSnapshot) = from(doc.id, doc.data.orEmpty(), legacy = false)

        /** [legacy] reads a Debt app document, whose amounts are whole taka rather than minor units. */
        fun from(id: String, data: Map<String, Any?>, legacy: Boolean) = Debt(
            id = id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            type = if (data["type"] == DEBT_BORROWED) DEBT_BORROWED else DEBT_LENT,
            amount = if (legacy) legacyAmount(data["amount"]) else (data["amount"] as? Number)?.toLong() ?: 0,
            originalAmount = if (legacy) legacyAmount(data["originalAmount"]) else (data["originalAmount"] as? Number)?.toLong() ?: 0,
            status = data["status"] as? String ?: "active",
            lastUpdated = data["lastUpdated"] as? String ?: "",
            records = (data["records"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let { m -> DebtRecord.from(m, legacy) } } ?: emptyList(),
            archived = data["archived"] as? Boolean ?: false,
        )
    }
}

/** Taka as a number or numeric string → minor units. */
private fun legacyAmount(value: Any?): Long = runCatching {
    when (value) {
        is Number -> BigDecimal(value.toString())
        is String -> BigDecimal(value.trim())
        else -> BigDecimal.ZERO
    }.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
}.getOrDefault(0)

/** The Debt app's rules, kept identical so imported histories keep adding up. */
object DebtLogic {
    fun nowIso(): String = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString()

    /** Replays history. Positive means I am owed (lent), negative means I owe (borrowed). */
    fun balance(records: List<DebtRecord>, startType: String): Long {
        var balance = 0L
        records.forEach { r ->
            when (r.action) {
                ACTION_INITIAL -> if (startType == DEBT_LENT) balance += r.amount else balance -= r.amount
                ACTION_INCREASE -> if (balance >= 0) balance += r.amount else balance -= r.amount
                ACTION_REPAY -> if (balance >= 0) balance -= r.amount else balance += r.amount
            }
        }
        return balance
    }

    /** Total paid back so far. */
    fun settled(records: List<DebtRecord>): Long = records.filter { it.action == ACTION_REPAY }.sumOf { it.amount }

    fun create(type: String, name: String, description: String, record: DebtRecord) = Debt(
        name = name, description = description, type = type, amount = record.amount, originalAmount = record.amount,
        status = "active", lastUpdated = nowIso(), records = listOf(record),
    )

    /** Adds an increase or repayment; paying past zero flips the debt to the other side. */
    fun append(debt: Debt, record: DebtRecord): Debt {
        val calculated = if (record.action == ACTION_REPAY) debt.amount - record.amount else debt.amount + record.amount
        return debt.copy(
            amount = abs(calculated),
            type = if (calculated < 0) other(debt.type) else debt.type,
            status = if (calculated == 0L) "closed" else "active",
            lastUpdated = nowIso(),
            records = debt.records + record,
        )
    }

    /** After a record was edited or removed: works out which side the history started on, then replays it. */
    fun recalculate(debt: Debt, records: List<DebtRecord>): Debt {
        val asLent = balance(debt.records, DEBT_LENT)
        val matchesLent = (asLent >= 0 && debt.type == DEBT_LENT && abs(asLent) == debt.amount) ||
            (asLent < 0 && debt.type == DEBT_BORROWED && abs(asLent) == debt.amount)
        val newBalance = balance(records, if (matchesLent) DEBT_LENT else DEBT_BORROWED)
        return debt.copy(
            amount = abs(newBalance),
            type = if (newBalance >= 0) DEBT_LENT else DEBT_BORROWED,
            status = if (newBalance == 0L) "closed" else "active",
            lastUpdated = nowIso(),
            records = records,
        )
    }

    fun other(type: String) = if (type == DEBT_LENT) DEBT_BORROWED else DEBT_LENT

    /** Date of the newest record, falling back to the day of the last update. */
    fun displayDate(debt: Debt): String =
        debt.records.maxByOrNull { it.date }?.date?.takeIf { it.isNotBlank() } ?: debt.lastUpdated.take(10)
}
