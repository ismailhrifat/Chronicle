package com.chronicle.expense.data

import com.google.firebase.firestore.DocumentSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class AccountType(val label: String) {
    CASH("Cash"), BANK("Bank"), CREDIT_CARD("Credit Card"), FDR("FDR"), DPS("DPS");

    companion object {
        fun of(value: String?) = entries.firstOrNull { it.name == value } ?: CASH
    }
}

enum class TxKind(val label: String) {
    EXPENSE("Expense"), INCOME("Income"), TRANSFER("Transfer"),

    /** Money leaving / entering an account through a debt record. Never counted as spending or income. */
    DEBT_OUT("Debt out"), DEBT_IN("Debt in");

    val isDebt get() = this == DEBT_OUT || this == DEBT_IN


    companion object {
        fun of(value: String?) = entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}

const val CATEGORY_OTHER = "Other"
const val CATEGORY_AUTO_LOG = "Auto Log"
const val CATEGORY_TRANSACTION_FEE = "Transaction Fee"
/** Shown as the destination of a transfer that has no "to" account. */
const val ACCOUNT_OTHER = "Other"

data class SmsRule(
    val sender: String = "",
    val sample: String = "",
    val kind: TxKind = TxKind.EXPENSE,
) {
    fun toMap() = mapOf("sender" to sender, "sample" to sample, "kind" to kind.name)

    companion object {
        fun from(map: Map<*, *>) = SmsRule(
            sender = map["sender"] as? String ?: "",
            sample = map["sample"] as? String ?: "",
            kind = TxKind.of(map["kind"] as? String),
        )
    }
}

data class Account(
    val id: String = "",
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    /** Minor units (poisha). */
    val balance: Long = 0,
    val color: Int = 0,
    val createdAt: Long = 0,
    val settlementDay: Int? = null,
    val dueDay: Int? = null,
    val remindDaysBefore: Int = 2,
    val smsRules: List<SmsRule> = emptyList(),
    /** Position on Home; null until the user reorders, then accounts sort by this. */
    val order: Long? = null,
) {
    val isCard get() = type == AccountType.CREDIT_CARD

    /** Everything except balance, which only changes through transactions. */
    fun settingsMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "type" to type.name,
        "color" to color,
        "settlementDay" to settlementDay,
        "dueDay" to dueDay,
        "remindDaysBefore" to remindDaysBefore,
        "smsRules" to smsRules.map { it.toMap() },
    )

    companion object {
        fun from(doc: DocumentSnapshot) = Account(
            id = doc.id,
            name = doc.getString("name") ?: "",
            type = AccountType.of(doc.getString("type")),
            balance = doc.getLong("balance") ?: 0,
            color = (doc.getLong("color") ?: 0).toInt(),
            createdAt = doc.getLong("createdAt") ?: 0,
            settlementDay = doc.getLong("settlementDay")?.toInt(),
            dueDay = doc.getLong("dueDay")?.toInt(),
            remindDaysBefore = (doc.getLong("remindDaysBefore") ?: 2).toInt(),
            smsRules = (doc.get("smsRules") as? List<*>)
                ?.mapNotNull { (it as? Map<*, *>)?.let(SmsRule::from) } ?: emptyList(),
            order = doc.getLong("order"),
        )
    }
}

data class Transaction(
    val id: String = "",
    val kind: TxKind = TxKind.EXPENSE,
    /** Minor units, always positive. */
    val amount: Long = 0,
    val accountId: String = "",
    val toAccountId: String? = null,
    val category: String = "",
    val description: String = "",
    val timestamp: Long = 0,
    val autoLogged: Boolean = false,
    /** Set when the expense belongs to an event; [category] then holds the event name. */
    val eventId: String? = null,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "kind" to kind.name,
        "amount" to amount,
        "accountId" to accountId,
        "toAccountId" to toAccountId,
        "category" to category,
        "description" to description,
        "timestamp" to timestamp,
        "autoLogged" to autoLogged,
        "eventId" to eventId,
    )

    /** Balance change per account caused by this transaction. */
    fun deltas(): Map<String, Long> = when (kind) {
        TxKind.EXPENSE, TxKind.DEBT_OUT -> mapOf(accountId to -amount)
        TxKind.INCOME, TxKind.DEBT_IN -> mapOf(accountId to amount)
        TxKind.TRANSFER -> buildMap {
            put(accountId, -amount)
            toAccountId?.let { put(it, (get(it) ?: 0) + amount) }
        }
    }

    companion object {
        fun from(doc: DocumentSnapshot) = Transaction(
            id = doc.id,
            kind = TxKind.of(doc.getString("kind")),
            amount = doc.getLong("amount") ?: 0,
            accountId = doc.getString("accountId") ?: "",
            toAccountId = doc.getString("toAccountId"),
            category = doc.getString("category") ?: "",
            description = doc.getString("description") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0,
            autoLogged = doc.getBoolean("autoLogged") ?: false,
            eventId = doc.getString("eventId"),
        )
    }
}

/** Net balance change per account when [old] is replaced by [new] (either may be null). */
fun balanceChanges(old: Transaction?, new: Transaction?): Map<String, Long> {
    val out = mutableMapOf<String, Long>()
    old?.deltas()?.forEach { (id, d) -> out[id] = (out[id] ?: 0) - d }
    new?.deltas()?.forEach { (id, d) -> out[id] = (out[id] ?: 0) + d }
    return out.filterValues { it != 0L }
}

data class Category(
    val id: String = "",
    val name: String = "",
    val kind: TxKind = TxKind.EXPENSE,
) {
    companion object {
        fun from(doc: DocumentSnapshot) = Category(
            id = doc.id,
            name = doc.getString("name") ?: "",
            kind = TxKind.of(doc.getString("kind")),
        )

        fun idFor(name: String, kind: TxKind) =
            kind.name.lowercase() + "_" + name.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-')
    }
}

/** A named group of expenses, e.g. a trip. */
data class Event(
    val id: String = "",
    val name: String = "",
    val closed: Boolean = false,
    val createdAt: Long = 0,
) {
    companion object {
        fun from(doc: DocumentSnapshot) = Event(
            id = doc.id,
            name = doc.getString("name") ?: "",
            closed = doc.getBoolean("closed") ?: false,
            createdAt = doc.getLong("createdAt") ?: 0,
        )
    }
}

object Money {
    const val SYMBOL = "৳"
    private val format = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val plain = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))

    /** `920300` → `৳9,203.00` */
    fun format(minor: Long, symbol: Boolean = true): String {
        val abs = format.format(BigDecimal(kotlin.math.abs(minor)).movePointLeft(2))
        val sign = if (minor < 0) "−" else ""
        return if (symbol) "$sign$SYMBOL$abs" else "$sign$abs"
    }

    /** For pre-filling an input field: `920300` → `9203`, `920350` → `9203.5` */
    fun toInput(minor: Long): String = plain.format(BigDecimal(minor).movePointLeft(2))

    /** Parses user or SMS text such as `9,203.00` into minor units. */
    fun parse(text: String): Long? {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty()) return null
        return try {
            BigDecimal(clean).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (e: Exception) {
            null
        }
    }
}
