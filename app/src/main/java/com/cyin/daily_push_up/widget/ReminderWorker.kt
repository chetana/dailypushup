package com.cyin.daily_push_up.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyin.daily_push_up.MainActivity
import com.cyin.daily_push_up.MyApplication
import com.cyin.daily_push_up.R
import java.util.Calendar

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "pushup_reminder"
        const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Push-up Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappel quotidien si les pompes ne sont pas encore validées"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 6) return Result.success() // Ne pas déranger la nuit

        val stats = MyApplication.database.pushUpDao().getStats()
        if (stats == null || stats.todayValidated) return Result.success()

        sendNotification()
        return Result.success()
    }

    private fun sendNotification() {
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💪 Daily Push-ups")
            .setContentText("Tu n'as pas encore validé tes pompes aujourd'hui !")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Tu n'as pas encore validé tes pompes aujourd'hui ! Quelques minutes suffisent pour garder le streak 🔥"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
