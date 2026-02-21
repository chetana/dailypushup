package com.cyin.daily_push_up

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cyin.daily_push_up.data.AppDatabase
import com.cyin.daily_push_up.widget.SyncWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "daily-push-up-db"
        ).fallbackToDestructiveMigration().build()

        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "pushup_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
}
