package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_MEAL
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val (title, body, id) = when (type) {
            TYPE_WATER -> Triple(
                "পানি পানের সময় হয়েছে!",
                "হাইড্রেটেড থাকতে এক গ্লাস (২৫০ মি.লি.) পানি পান করে লগ করুন।",
                1002
            )
            TYPE_WEIGHT -> Triple(
                "সকালের ওজন লগ করুন",
                "নিয়মিত প্রোগ্রেস ট্র্যাক করতে আজকের ওজন রেকর্ড করুন।",
                1003
            )
            else -> Triple(
                "খাবার লগ করার রিমাইন্ডার",
                "আপনার আজকের খাবার ও ক্যালোরি NutriPulse AI-তে যোগ করতে ভুলবেন না!",
                1001
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "nutripulse_reminders"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_MEAL = "meal"
        const val TYPE_WATER = "water"
        const val TYPE_WEIGHT = "weight"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "স্বাস্থ্য ও পুষ্টি রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "খাবার, পানি ও ওজন লগ করার দৈনিক রিমাইন্ডার"
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            }
        }
    }
}
