package com.cyin.daily_push_up.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cyin.daily_push_up.MainActivity
import com.cyin.daily_push_up.MyApplication
import com.cyin.daily_push_up.R
import com.cyin.daily_push_up.api.RetrofitClient
import com.cyin.daily_push_up.api.ValidateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PushUpWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_VALIDATE = "com.cyin.daily_push_up.ACTION_VALIDATE"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, PushUpWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PushUpWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_VALIDATE) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.api.validate(ValidateRequest(30))
                    val dao = MyApplication.database.pushUpDao()
                    val stats = RetrofitClient.api.getStats()
                    dao.insertStats(
                        com.cyin.daily_push_up.data.CachedStats(
                            totalPushups = stats.totalPushups,
                            totalDays = stats.totalDays,
                            currentStreak = stats.currentStreak,
                            longestStreak = stats.longestStreak,
                            todayValidated = stats.todayValidated,
                            todayTarget = stats.todayTarget,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                } catch (_: Exception) {
                }
                updateAllWidgets(context)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_pushup)

        // Open app on tap
        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetStreak, openPending)
        views.setOnClickPendingIntent(R.id.widgetStatus, openPending)

        // Validate button
        val validateIntent = Intent(context, PushUpWidgetProvider::class.java).apply {
            action = ACTION_VALIDATE
        }
        val validatePending = PendingIntent.getBroadcast(
            context, 1, validateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetValidateBtn, validatePending)

        // Load stats from Room
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = MyApplication.database.pushUpDao()
                val stats = dao.getStats()
                if (stats != null) {
                    views.setTextViewText(R.id.widgetStreak, stats.currentStreak.toString())
                    if (stats.todayValidated) {
                        views.setTextViewText(R.id.widgetStatus, "🎉 Done today!")
                        views.setTextViewText(R.id.widgetValidateBtn, "✅ Done")
                    } else {
                        views.setTextViewText(R.id.widgetStatus, "💪 Not done yet")
                        views.setTextViewText(R.id.widgetValidateBtn, "✅ Validate")
                    }
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
