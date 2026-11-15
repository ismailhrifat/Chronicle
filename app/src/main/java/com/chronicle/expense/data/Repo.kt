package com.chronicle.expense.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * All Firestore access for one signed-in user. Writes are not awaited: Firestore applies
 * them to the local cache immediately and syncs when the network allows.
 */
open class Repo(val uid: String, private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val root = db.collection("users").document(uid)
    private val accountsCol = root.collection("accounts")
    private val txCol = root.collection("transactions")
    private val categoriesCol = root.collection("categories")
    private val eventsCol = root.collection("events")
    private val debtsCol = root.collection("debts")

    // ---- Reads ------------------------------------------------------------------------

    open fun accounts(): Flow<List<Account>> =
        accountsCol.orderBy("createdAt").listen(Account::from)

    open fun categories(): Flow<List<Category>> =
        categoriesCol.listen(Category::from)

    /** Newest first. [from] inclusive, [to] exclusive, both in epoch millis. */
    open fun transactions(from: Long? = null, to: Long? = null, limit: Long? = null): Flow<List<Transaction>> {
        var q: Query = txCol
        if (from != null) q = q.whereGreaterThanOrEqualTo("timestamp", from)
        if (to != null) q = q.whereLessThan("timestamp", to)
        q = q.orderBy("timestamp", Query.Direction.DESCENDING)
        if (limit != null) q = q.limit(limit)
        return q.listen(Transaction::from)
    }

    open suspend fun accountsOnce(): List<Account> {
        val snap = withTimeoutOrNull(6_000) { runCatching { accountsCol.get().await() }.getOrNull() }
            ?: runCatching { accountsCol.get(Source.CACHE).await() }.getOrNull()
        return snap?.documents?.map(Account::from) ?: emptyList()
    }

    open suspend fun transaction(id: String): Transaction? {
        val ref = txCol.document(id)
        val doc = runCatching { ref.get(Source.CACHE).await() }.getOrNull()
            ?: withTimeoutOrNull(6_000) { runCatching { ref.get().await() }.getOrNull() }
        return doc?.takeIf { it.exists() }?.let(Transaction::from)
    }

    suspend fun transactionExists(id: String): Boolean = transaction(id) != null

    // ---- Accounts ---------------------------------------------------------------------

    open fun addAccount(name: String, type: AccountType, initialBalance: Long, color: Int): String {
        val ref = accountsCol.document()
        ref.set(
            Account(name = name.trim(), type = type, color = color).settingsMap() + mapOf(
                "balance" to initialBalance,
                "createdAt" to System.currentTimeMillis(),
            )
        )
        return ref.id
    }

    open fun saveAccountSettings(account: Account) {
        accountsCol.document(account.id).set(account.settingsMap(), SetOptions.merge())
    }

    open fun reorderAccounts(ids: List<String>) {
        val batch = db.batch()
        ids.forEachIndexed { i, id -> batch.update(accountsCol.document(id), "order", i.toLong()) }
        batch.commit()
    }

    /** Removes the account and its transactions; undoes transfers on the other side. */
    open suspend fun deleteAccount(account: Account, otherAccountIds: Set<String>) {
        val own = runCatching { txCol.whereEqualTo("accountId", account.id).get().await().documents }.getOrDefault(emptyList())
        val incoming = runCatching { txCol.whereEqualTo("toAccountId", account.id).get().await().documents }.getOrDefault(emptyList())
        val refunds = mutableMapOf<String, Long>()
        (own + incoming).distinctBy { it.id }.map(Transaction::from).forEach { tx ->
            balanceChanges(tx, null).forEach { (id, delta) ->
                if (id != account.id && id in otherAccountIds) refunds[id] = (refunds[id] ?: 0) + delta
            }
        }
        val refs = (own + incoming).distinctBy { it.id }.map { it.reference }
        refs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach(batch::delete)
            batch.commit()
        }
        val batch = db.batch()
        refunds.forEach { (id, delta) -> batch.update(accountsCol.document(id), "balance", FieldValue.increment(delta)) }
        batch.delete(accountsCol.document(account.id))
        batch.commit()
    }

    // ---- Transactions -----------------------------------------------------------------

    /**
     * Creates ([old] == null) or replaces a transaction and moves account balances to match.
     * [knownAccounts] guards against touching accounts that no longer exist.
     */
    open fun saveTransaction(old: Transaction?, new: Transaction, knownAccounts: Set<String>, id: String? = null): String {
        val ref = when {
            old != null -> txCol.document(old.id)
            id != null -> txCol.document(id)
            else -> txCol.document()
        }
        val batch = db.batch()
        batch.set(ref, new.toMap())
        balanceChanges(old, new).forEach { (accountId, delta) ->
            if (accountId in knownAccounts) {
                batch.update(accountsCol.document(accountId), "balance", FieldValue.increment(delta))
            }
        }
        batch.commit()
        return ref.id
    }

    open fun deleteTransaction(old: Transaction, knownAccounts: Set<String>) {
        val batch = db.batch()
        batch.delete(txCol.document(old.id))
        balanceChanges(old, null).forEach { (accountId, delta) ->
            if (accountId in knownAccounts) {
                batch.update(accountsCol.document(accountId), "balance", FieldValue.increment(delta))
            }
        }
        batch.commit()
    }

    // ---- Categories -------------------------------------------------------------------

    open fun ensureCategory(name: String, kind: TxKind) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        categoriesCol.document(Category.idFor(clean, kind))
            .set(mapOf("name" to clean, "kind" to kind.name), SetOptions.merge())
    }

    open fun deleteCategory(category: Category) {
        categoriesCol.document(category.id).delete()
    }

    /** Renames the category and relabels the transactions already filed under it. */
    open suspend fun renameCategory(category: Category, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean == category.name) return
        categoriesCol.document(category.id).delete()
        ensureCategory(clean, category.kind)
        val docs = runCatching {
            txCol.whereEqualTo("category", category.name).whereEqualTo("kind", category.kind.name).get().await().documents
        }.getOrDefault(emptyList())
        docs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(it.reference, "category", clean) }
            batch.commit()
        }
    }

    /** First run gets a starter set; reserved names are always kept present. */
    open fun seedCategories(existing: List<Category>, firstRun: Boolean) {
        val have = existing.map { it.id }.toSet()
        val wanted = buildList {
            listOf(TxKind.EXPENSE, TxKind.INCOME).forEach { kind ->
                add(CATEGORY_OTHER to kind)
                add(CATEGORY_AUTO_LOG to kind)
            }
            if (firstRun) {
                listOf("Food", "Groceries", "Transport", "Rent", "Utilities", "Shopping", "Health", "Entertainment")
                    .forEach { add(it to TxKind.EXPENSE) }
                listOf("Salary", "Freelance", "Gift", "Interest").forEach { add(it to TxKind.INCOME) }
            }
        }
        wanted.filter { (name, kind) -> Category.idFor(name, kind) !in have }
            .forEach { (name, kind) -> ensureCategory(name, kind) }
    }

    // ---- Events -----------------------------------------------------------------------

    open fun events(): Flow<List<Event>> =
        eventsCol.orderBy("createdAt", Query.Direction.DESCENDING).listen(Event::from)

    /** Every expense that belongs to any event, unordered. */
    open fun eventTransactions(): Flow<List<Transaction>> =
        txCol.whereNotEqualTo("eventId", null).listen(Transaction::from)

    open fun addEvent(name: String): String {
        val ref = eventsCol.document()
        ref.set(mapOf("name" to name.trim(), "closed" to false, "createdAt" to System.currentTimeMillis()))
        return ref.id
    }

    open fun setEventClosed(id: String, closed: Boolean) {
        eventsCol.document(id).update("closed", closed)
    }

    /** Removes the event only; its expenses stay in history as ordinary expenses. */
    open fun deleteEvent(id: String) {
        eventsCol.document(id).delete()
    }

    open suspend fun renameEvent(event: Event, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean == event.name) return
        eventsCol.document(event.id).update("name", clean)
        val docs = runCatching { txCol.whereEqualTo("eventId", event.id).get().await().documents }.getOrDefault(emptyList())
        docs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(it.reference, "category", clean) }
            batch.commit()
        }
    }

    // ---- Debts ------------------------------------------------------------------------

    open fun debts(): Flow<List<Debt>> = debtsCol.listen(Debt::from)

    /** Creates the document when [Debt.id] is blank. */
    open fun saveDebt(debt: Debt): String {
        val ref = if (debt.id.isBlank()) debtsCol.document() else debtsCol.document(debt.id)
        ref.set(debt.toMap())
        return ref.id
    }

    open fun setDebtArchived(id: String, archived: Boolean) {
        debtsCol.document(id).update("archived", archived)
    }

    open fun deleteDebt(id: String) {
        debtsCol.document(id).delete()
    }

    private fun <T> Query.listen(map: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snap != null) trySend(snap.documents.map(map))
        }
        awaitClose { registration.remove() }
    }
}
