package com.example.detectionexample.models

import java.nio.ByteBuffer

data class BitmapProxy (val buffer: ByteBuffer, val width: Int, val height: Int, val rotationDegrees: Int, val timestamp: Long, val flipX: Boolean = false, val flipY:Boolean = false)
