package com.example.detectionexample.config

import java.text.DateFormat
import java.util.*

object CameraConfig {


    const val IMAGE_MIMETYPE = "image/jpeg"
    const val IMAGE_FOLDER = "Pictures/CameraX-Image"
    const val VIDEO_MIMETYPE = "video/avc"
    const val VIDEO_FOLDER = "Movies/CameraX-Video"
    const val TAG = "CameraX"
    val FILENAME: String
        get() = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(Date())
    const val KEY_CAPTURE_IMAGE = "CAPTURE IMAGE"
    const val KEY_CAPTURE_TRACKER_OBJECT = "CAPTURE_TRACKER_OBJECT"
}