package com.financeiro.financeiro

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

data class DailyReminderTime(
    val hour: Int,
    val minute: Int
) {
    fun toLocalTime(): LocalTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

object NotificationScheduler {
    const val PREFS_NAME = "finance_notifications"
    const val KEY_ENABLED = "notifications_enabled"
    const val KEY_HOUR = "notification_hour"
    const val KEY_MINUTE = "notification_minute"
    const val INPUT_RUN_MODE = "run_mode"
    const val RUN_MODE_SCHEDULED = "scheduled"
    const val RUN_MODE_CATCH_UP = "catch_up"

    private const val DEFAULT_NOTIFICATION_HOUR = 10
    private const val DEFAULT_NOTIFICATION_MINUTE = 0
    private const val DAILY_WORK_NAME = "daily_launch_reminder_work"
    private const val IMMEDIATE_WORK_NAME = "daily_launch_reminder_immediate_check"

    fun scheduleDailyLaunchReminder(context: Context, reminderTime: DailyReminderTime) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<DailyLaunchReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayToTime(reminderTime), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(INPUT_RUN_MODE to RUN_MODE_SCHEDULED))
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyLaunchReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    fun scheduleImmediateMorningCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = OneTimeWorkRequestBuilder<DailyLaunchReminderWorker>()
            .setInputData(workDataOf(INPUT_RUN_MODE to RUN_MODE_CATCH_UP))
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun isDailyReminderEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun getDailyReminderTime(context: Context): DailyReminderTime {
        return DailyReminderTime(
            hour = prefs(context).getInt(KEY_HOUR, DEFAULT_NOTIFICATION_HOUR).coerceIn(0, 23),
            minute = prefs(context).getInt(KEY_MINUTE, DEFAULT_NOTIFICATION_MINUTE).coerceIn(0, 59)
        )
    }

    fun updateDailyReminderSettings(context: Context, enabled: Boolean, reminderTime: DailyReminderTime) {
        val cleanedTime = DailyReminderTime(
            hour = reminderTime.hour.coerceIn(0, 23),
            minute = reminderTime.minute.coerceIn(0, 59)
        )
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, cleanedTime.hour)
            .putInt(KEY_MINUTE, cleanedTime.minute)
            .apply()

        if (enabled) {
            scheduleDailyLaunchReminder(context, cleanedTime)
            scheduleImmediateMorningCheck(context)
        } else {
            cancelDailyLaunchReminder(context)
        }
    }

    fun rescheduleFromStoredSettings(context: Context) {
        if (isDailyReminderEnabled(context)) {
            val reminderTime = getDailyReminderTime(context)
            scheduleDailyLaunchReminder(context, reminderTime)
            scheduleImmediateMorningCheck(context)
        } else {
            cancelDailyLaunchReminder(context)
        }
    }

    private fun initialDelayToTime(reminderTime: DailyReminderTime): Long {
        val localTime = reminderTime.toLocalTime()
        val now = LocalDateTime.now()
        val todayAtHour = now
            .withHour(localTime.hour)
            .withMinute(localTime.minute)
            .withSecond(0)
            .withNano(0)
        val nextRun = if (now.isBefore(todayAtHour)) todayAtHour else todayAtHour.plusDays(1)
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
