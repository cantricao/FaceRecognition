package com.example.detectionexample.engage

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Orchestrator for scheduling periodic background sync and immediate one-time publish jobs.
 */
object EngagePublisher {

    fun publishPeriodically(context: Context, publishType: String) {
        val workRequest = PeriodicWorkRequestBuilder<EngageWorker>(Constants.REPEAT_INTERVAL, TimeUnit.HOURS)
            .setInputData(workDataOf(Constants.PUBLISH_TYPE_KEY to publishType))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "EngagePeriodic_$publishType",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun publishOneTime(context: Context, publishType: String) {
        val workRequest = OneTimeWorkRequestBuilder<EngageWorker>()
            .setInputData(workDataOf(Constants.PUBLISH_TYPE_KEY to publishType))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "EngageOneTime_$publishType",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
