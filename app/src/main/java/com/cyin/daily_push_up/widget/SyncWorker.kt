package com.cyin.daily_push_up.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyin.daily_push_up.MyApplication
import com.cyin.daily_push_up.data.PushUpRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = PushUpRepository(MyApplication.database.pushUpDao())
        val result = repo.sync()
        PushUpWidgetProvider.updateAllWidgets(applicationContext)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
