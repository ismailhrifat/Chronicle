package com.chronicle.expense.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val sender = parts[0].displayOriginatingAddress ?: return
        val body = parts.joinToString("") { it.displayMessageBody ?: "" }
        if (body.isBlank()) return

        val work = OneTimeWorkRequestBuilder<SmsWorker>()
            .setInputData(
                workDataOf(
                    SmsWorker.KEY_SENDER to sender,
                    SmsWorker.KEY_BODY to body.take(2000),
                    SmsWorker.KEY_TIME to parts[0].timestampMillis,
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }
}
