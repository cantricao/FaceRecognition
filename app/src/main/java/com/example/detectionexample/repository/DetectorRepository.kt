package com.example.detectionexample.repository

import android.graphics.Bitmap
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Singleton
interface DetectorRepository {
    fun setThreshold(threshold: Float){
    }

    fun detectInImage(bitmap: Bitmap): Flow<List<Recognition>>

    fun setFileModelName(filename: String) {

    }

    fun setModelDevice(name: String) {

    }
}