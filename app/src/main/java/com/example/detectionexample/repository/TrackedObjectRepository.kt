package com.example.detectionexample.repository

import android.graphics.Color
import android.graphics.RectF
import com.example.detectionexample.models.Recognition
import com.example.detectionexample.models.TrackedRecognition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackedObjectRepository @Inject constructor() {
    private var _trackedObjects: Flow<List<TrackedRecognition>> =
        flow { emptyList<TrackedRecognition>() }
    val trackedObjects
        get() = _trackedObjects

    companion object {
        private val COLORS = intArrayOf(
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
        )
    }

    fun trackResults(results: Flow<List<Recognition>>) {
        processResults(results)
    }

    private fun processResults(results: Flow<List<Recognition>>) {
        _trackedObjects = results.map { it.mapIndexed { index, potential ->
            TrackedRecognition(
                location = RectF(potential.location),
                detectionConfidence = potential.confidence,
                color = COLORS[index],
                title = potential.title,
                landmark = potential.landmark
            )
        } }
    }
}