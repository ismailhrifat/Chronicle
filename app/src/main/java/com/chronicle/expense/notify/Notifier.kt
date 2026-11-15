package com.chronicle.expense.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chronicle.expense.MainActivity
import com.chronicle.expense.R
import com.chronicle.expense.data.ACCOUNT_OTHER
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind

object Notifier {
    const val EXTRA_EDIT_TX = "edit_tx"
    private const val CHANNEL_AUTO_LOG = "auto_log"
    private const val CHANNEL_REMINDERS = "reminders"
    private const val BRAND = 0xFF3F6B5C.toInt()

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_AUTO_LOG, "Auto-logged transactions", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Transactions recorded from bank SMS alerts" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Card reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Credit card settlement and payment due dates" }
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    fun autoLogged(context: Context, txId: String, tx: Transaction, accountName: String) {
        val title = "${Money.format(tx.amount)} ${tx.kind.label.lowercase()} logged"
        val text = if (tx.kind == TxKind.TRANSFER) "$accountName → $ACCOUNT_OTHER · Tap to set the destination account"
        else "$accountName · Tap to set category and description"
        post(context, CHANNEL_AUTO_LOG, txId.hashCode(), title, text, openIntent(context, txId))
    }

    fun reminder(context: Context, id: Int, title: String, text: String) {
        post(context, CHANNEL_REMINDERS, id, title, text, openIntent(context, null))
    }

    private fun openIntent(context: Context, txId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (txId != null) {
                putExtra(EXTRA_EDIT_TX, txId)
                action = "edit:$txId" // keeps PendingIntents distinct
            }
        }
        return PendingIntent.getActivity(
            context, txId?.hashCode() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun post(context: Context, channel: String, id: Int, title: String, text: String, intent: PendingIntent) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_chronicle)
            .setColor(BRAND)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
