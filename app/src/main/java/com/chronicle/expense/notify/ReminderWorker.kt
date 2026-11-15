package com.chronicle.expense.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chronicle.expense.data.Account
import com.chronicle.expense.data.Repo
import com.google.firebase.auth.FirebaseAuth
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Runs daily; nudges before and on credit card settlement and due dates. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val today = LocalDate.now()
        val prefs = applicationContext.getSharedPreferences("reminders", Context.MODE_PRIVATE)

        Repo(uid).accountsOnce().filter { it.isCard }.forEach { card ->
            check(card, "settlement", card.settlementDay, today, prefs)
            check(card, "due", card.dueDay, today, prefs)
        }
        return Result.success()
    }

    private fun check(card: Account, kind: String, day: Int?, today: LocalDate, prefs: android.content.SharedPreferences) {
        if (day == null) return
        val date = nextOccurrence(day, today)
        val daysLeft = ChronoUnit.DAYS.between(today, date).toInt()
        val stage = when {
            daysLeft == 0 -> "today"
            daysLeft <= card.remindDaysBefore -> "early"
            else -> return
        }
        val key = "${card.id}:$kind:$date:$stage"
        if (prefs.getBoolean(key, false)) return

        val pretty = date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
        val whenText = when (daysLeft) {
            0 -> "today"
            1 -> "tomorrow"
            else -> "in $daysLeft days"
        }
        val (title, text) = if (kind == "due") {
            "${card.name} payment due $whenText" to "Pay by $pretty to avoid late fees."
        } else {
            "${card.name} statement closes $whenText" to "Settlement date is $pretty."
        }
        Notifier.reminder(applicationContext, key.hashCode(), title, text)
        prefs.edit().putBoolean(key, true).apply()
    }

    companion object {
        fun nextOccurrence(day: Int, today: LocalDate): LocalDate {
            val thisMonth = today.withDayOfMonth(minOf(day, today.lengthOfMonth()))
            if (!thisMonth.isBefore(today)) return thisMonth
            val next = today.plusMonths(1)
            return next.withDayOfMonth(minOf(day, next.lengthOfMonth()))
        }

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var first = now.withHour(9).withMinute(0).withSecond(0)
            if (!first.isAfter(now)) first = first.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, first).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("card_reminders", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Used after saving card dates so a reminder that is already due is not missed. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>().build())
        }
    }
}
