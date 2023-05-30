package com.example.detectionexample.detector

import android.graphics.Bitmap

interface DetectorDataSource {
    fun detectInImage(image: Bitmap)
    fun clearObjectDetector()

}