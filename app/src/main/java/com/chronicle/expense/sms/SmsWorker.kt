package com.chronicle.expense.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chronicle.expense.data.CATEGORY_AUTO_LOG
import com.chronicle.expense.data.Repo
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.notify.Notifier
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.security.MessageDigest

/** Matches an incoming SMS against every account's auto-log rules and records the first hit. */
class SmsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.success()
        val body = inputData.getString(KEY_BODY) ?: return Result.success()
        val time = inputData.getLong(KEY_TIME, 0)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val repo = Repo(uid)
        val accounts = repo.accountsOnce()
        for (account in accounts) {
            for (rule in account.smsRules) {
                if (!SmsPattern.senderMatches(rule.sender, sender)) continue
                val amount = SmsPattern.compile(rule.sample)?.extract(body) ?: continue

                val id = "sms_" + sha1("$sender|$body|$time").take(24)
                if (repo.transactionExists(id)) return Result.success()

                val tx = Transaction(
                    kind = rule.kind,
                    amount = amount,
                    accountId = account.id,
                    // A transfer leaves this account for "Other" until the user picks the destination.
                    toAccountId = null,
                    category = if (rule.kind == TxKind.TRANSFER) "Transfer" else CATEGORY_AUTO_LOG,
                    description = "",
                    timestamp = System.currentTimeMillis(),
                    autoLogged = true,
                )
                repo.saveTransaction(null, tx, accounts.map { it.id }.toSet(), id = id)
                Notifier.autoLogged(applicationContext, id, tx, account.name)
                delay(1500) // let Firestore flush the write to disk before the process may be reclaimed
                return Result.success()
            }
        }
        return Result.success()
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIME = "time"
    }
}
