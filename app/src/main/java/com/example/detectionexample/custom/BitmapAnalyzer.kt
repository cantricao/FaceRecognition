package com.example.detectionexample.custom

import android.graphics.Bitmap

interface BitmapAnalyzer {
    fun analyze(image: Bitmap, timestamp: Long)
}