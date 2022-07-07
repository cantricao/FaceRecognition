package com.example.detectionexample.models

import android.graphics.PointF
import android.graphics.RectF

/** An immutable result returned by a Detector describing what was recognized.  */
data class Recognition(
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    var id: String,
    /** Display name for the recognition.  */
    var title: String,
    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    var confidence: Float,
    /** Optional location within the source image for the location of the recognized object.  */
    var location: RectF,

    var landmark: List<PointF> = listOf()
) {

    override fun toString(): String {
        val resultString = "[$id] $title ${String.format("(%.1f%%) ", confidence)} $location "
        return resultString.trim { it <= ' ' }
    }

}