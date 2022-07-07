package com.example.detectionexample.detector

import android.graphics.Bitmap
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow

interface DetectorDataSource {
    fun detectInImage(image: Bitmap): Flow<List<Recognition>>
}