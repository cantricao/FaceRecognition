package com.example.detectionexample

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), CameraXConfig.Provider, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Play Engage dynamic receiver registration (required in addition to static manifest declaration)
        com.example.detectionexample.engage.EngageBroadcastReceiver.register(this)
        com.example.detectionexample.engage.EngagePublisher.publishPeriodically(
            this,
            com.example.detectionexample.engage.Constants.PUBLISH_TYPE_RECOMMENDATIONS
        )
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setExecutor(Executors.newSingleThreadExecutor())
            .setMinimumLoggingLevel(Log.VERBOSE)
            .setWorkerFactory(workerFactory)
            .build()
}
