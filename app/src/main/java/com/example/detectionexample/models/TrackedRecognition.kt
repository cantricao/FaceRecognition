@file:UseSerializers(PointFSerializer::class)
package com.example.detectionexample.models
import android.graphics.PointF
import android.graphics.RectF
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class TrackedRecognition (
    @Serializable(with = RectFSerializer::class)
    var location: RectF,
    var detectionConfidence: Float,
    var color: Int,
    var title: String,
    var landmark: List<PointF> = listOf()
)