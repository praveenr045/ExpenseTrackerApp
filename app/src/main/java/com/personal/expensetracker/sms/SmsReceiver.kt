package com.personal.expensetracker.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.personal.expensetracker.data.model.Expense
import com.personal.expensetracker.data.model.ExpenseSource
import com.personal.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ExpenseRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (sms in messages) {
            val sender = sms.originatingAddress ?: continue
            val body = sms.messageBody ?: continue

            if (!SmsParser.isBankSms(sender)) continue
            if (!SmsParser.isDebitSms(body)) continue

            val parsed = SmsParser.parse(body) ?: continue

            CoroutineScope(Dispatchers.IO).launch {
                // Avoid duplicate entries if SMS is received in multiple parts
                if (repository.isDuplicateSms(body)) return@launch

                val expense = Expense(
                    amount = parsed.amount,
                    description = parsed.description,
                    merchant = parsed.merchant,
                    category = parsed.category,
                    source = ExpenseSource.SMS,
                    rawSms = body
                )
                repository.insertExpense(expense)
                showNotification(context, parsed.amount, parsed.merchant)
            }
        }
    }

    private fun showNotification(context: Context, amount: Double, merchant: String) {
        val channelId = "expense_auto"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Auto-detected Expenses",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val merchantText = if (merchant.isNotBlank()) " at $merchant" else ""
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Expense Added ₹${"%.2f".format(amount)}")
            .setContentText("Debit of ₹${"%.2f".format(amount)}$merchantText auto-recorded.")
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}