package com.example.detectionexample.detector

import android.graphics.Bitmap
import android.media.Image
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow

interface DetectorDataSource {
    fun detectInImage(image: Bitmap): Flow<List<Recognition>>
    fun detectInImage(image: Image): Flow<List<Recognition>>
}