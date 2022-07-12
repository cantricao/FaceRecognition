package com.example.detectionexample.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.detectionexample.detector.FaceCascadeOpencvDetector
import com.example.detectionexample.models.Recognition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpencvRepository @Inject constructor(@ApplicationContext val context: Context): DetectorRepository {

    private val _detector: FaceCascadeOpencvDetector by lazy { FaceCascadeOpencvDetector(context) }

    override fun setThreshold(threshold: Float) {
    }

    override fun detectInImage(bitmap: Bitmap): Flow<List<Recognition>> {
        return _detector.detectInImage(bitmap)
    }

    override fun setFileModelName(filename: String) {
    }

    override fun setModelDevice(name: String) {
    }
}