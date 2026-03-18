package com.financeiro.financeiro

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financeiro.financeiro.data.FinanceDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.Normalizer

class DailyLaunchReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val configuredTime = NotificationScheduler.getDailyReminderTime(applicationContext)
        if (!shouldRunNow(configuredTime)) return Result.success()

        val today = LocalDate.now()
        val startMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val database = FinanceDatabase.getInstance(applicationContext)
        val accounts = database.personDao().listAll()
        if (accounts.isEmpty()) return Result.success()

        val concludedIdsByAccount = accounts.associate { account ->
            account.id to loadConcludedIds(account.id)
        }
        val allItems = database.transactionDao().listAll()

        val todayCount = allItems.count { item ->
            val dueMillis = referenceDateMillisForFuture(item)
            val concludedIds = concludedIdsByAccount[item.accountId].orEmpty()
            dueMillis in startMillis..endMillis && item.id !in concludedIds
        }
        if (todayCount <= 0) return Result.success()

        val prefs = applicationContext.getSharedPreferences(NotificationScheduler.PREFS_NAME, Context.MODE_PRIVATE)
        val dayKey = today.format(DateTimeFormatter.BASIC_ISO_DATE)
        val lastDay = prefs.getString("last_notified_day", "")
        val lastCount = prefs.getInt("last_notified_count", 0)
        if (lastDay == dayKey && todayCount <= lastCount) {
            return Result.success()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        createChannelIfNeeded()
        showNotification(todayCount)

        prefs.edit()
            .putString("last_notified_day", dayKey)
            .putInt("last_notified_count", todayCount)
            .remove("last_notified_account_id")
            .apply()

        return Result.success()
    }

    private fun shouldRunNow(configuredTime: DailyReminderTime): Boolean {
        val runMode = inputData.getString(NotificationScheduler.INPUT_RUN_MODE)
            ?: NotificationScheduler.RUN_MODE_SCHEDULED
        return when (runMode) {
            NotificationScheduler.RUN_MODE_CATCH_UP ->
                !LocalTime.now().isBefore(configuredTime.toLocalTime())
            else -> true
        }
    }

    private fun referenceDateMillisForFuture(item: com.financeiro.financeiro.data.FinancialTransactionEntity): Long {
        return if (isCardPaymentMethod(item.paymentMethod)) {
            item.cardPaymentDateMillis ?: item.dateMillis
        } else {
            item.dateMillis
        }
    }

    private fun isCardPaymentMethod(paymentMethod: String): Boolean {
        if (paymentMethod.isBlank()) return false
        val normalized = Normalizer
            .normalize(paymentMethod.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return normalized.contains("cartao")
    }

    private fun loadConcludedIds(accountId: Long): Set<Long> {
        val prefs = applicationContext.getSharedPreferences("financeiro_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("concluded_dates_$accountId", "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw
            .split("|||")
            .mapNotNull { entry ->
                val parts = entry.split("::", limit = 2)
                parts.firstOrNull()?.trim()?.toLongOrNull()
            }
            .toSet()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Contas vencendo hoje",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos das contas que vencem hoje."
        }
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(todayCount: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (todayCount == 1) {
            "Você tem 1 lançamento vencendo hoje."
        } else {
            "Você tem $todayCount lançamentos vencendo hoje."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Financeiro")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text Toque para abrir o app."))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "daily_launch_reminder"
        private const val NOTIFICATION_ID = 1001
    }
}
