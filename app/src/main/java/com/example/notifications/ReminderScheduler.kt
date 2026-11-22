package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    fun syncReminders(
        context: Context,
        mealEnabled: Boolean,
        waterEnabled: Boolean,
        weightEnabled: Boolean
    ) {
        ReminderReceiver.createNotificationChannel(context)

        if (mealEnabled) {
            scheduleDaily(context, 13, 0, ReminderReceiver.TYPE_MEAL, 2001)
        } else {
            cancel(context, ReminderReceiver.TYPE_MEAL, 2001)
        }

        if (waterEnabled) {
            scheduleInterval(
                context = context,
                intervalMillis = AlarmManager.INTERVAL_HOUR * 3,
                type = ReminderReceiver.TYPE_WATER,
                requestCode = 2002
            )
        } else {
            cancel(context, ReminderReceiver.TYPE_WATER, 2002)
        }

        if (weightEnabled) {
            scheduleDaily(context, 8, 0, ReminderReceiver.TYPE_WEIGHT, 2003)
        } else {
            cancel(context, ReminderReceiver.TYPE_WEIGHT, 2003)
        }
    }

    private fun scheduleDaily(
        context: Context,
        hour: Int,
        minute: Int,
        type: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val pi = buildPendingIntent(context, type, requestCode)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    private fun scheduleInterval(
        context: Context,
        intervalMillis: Long,
        type: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + intervalMillis
        val pi = buildPendingIntent(context, type, requestCode)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intervalMillis,
            pi
        )
    }

    private fun cancel(context: Context, type: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = buildPendingIntent(context, type, requestCode)
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(context: Context, type: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
