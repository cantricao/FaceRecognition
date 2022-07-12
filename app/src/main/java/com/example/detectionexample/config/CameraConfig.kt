package com.example.detectionexample.config

import java.text.DateFormat
import java.util.*

object CameraConfig {


    const val MIMETYPE = "image/jpeg"
    const val FOLDER = "Pictures/CameraX-Image"
    const val TAG = "CameraX"
    val FILENAME: String
        get() = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(Date())
    const val KEY_CAPTURE_IMAGE = "CAPTURE IMAGE"
    const val KEY_CAPTURE_TRACKER_OBJECT = "CAPTURE_TRACKER_OBJECT"

    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0
}