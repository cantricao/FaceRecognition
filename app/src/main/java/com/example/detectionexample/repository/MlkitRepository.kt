package com.example.detectionexample.repository

import android.graphics.Bitmap
import com.example.detectionexample.detector.FaceMlkitDetector
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlkitRepository @Inject constructor() :DetectorRepository {
    private var _detector: FaceMlkitDetector = FaceMlkitDetector()

    override fun detectInImage(bitmap: Bitmap): Flow<List<Recognition>> {
        return _detector.detectInImage(bitmap)
    }
}
