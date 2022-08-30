package com.example.detectionexample.custom

import android.graphics.Bitmap

interface BitmapEncoding {
    fun queueFrame(bitmap: Bitmap)
}