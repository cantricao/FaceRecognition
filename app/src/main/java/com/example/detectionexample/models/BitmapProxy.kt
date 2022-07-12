package com.example.detectionexample.models

import java.nio.IntBuffer

data class BitmapProxy(
    val buffer: IntBuffer,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val timestamp: Long,
    val flipX: Boolean = false,
    val flipY:Boolean = false)
