package com.example.detectionexample.domain

import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.repository.DetectorRepository
import com.example.detectionexample.repository.MlkitRepository
import com.example.detectionexample.repository.OpencvRepository
import com.example.detectionexample.repository.TfliteRepository
import javax.inject.Inject

class DetectorUsecase @Inject constructor(private val mlkitRepository: MlkitRepository, private val opencvRepository: OpencvRepository, private val tfliteRepository: TfliteRepository) {
    operator fun invoke(modelname: String): DetectorRepository {
        return when (modelname) {
            ModelConfig.MLKIT_CODENAME -> mlkitRepository
            ModelConfig.OPENCV_CODENAME -> opencvRepository
            else -> tfliteRepository
        }
    }
}