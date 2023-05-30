package com.example.detectionexample.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.extractor.TfliteExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorRepository @Inject constructor(@ApplicationContext val context: Context): Closeable {
    private var device = ModelConfig.EXTRACTOR_DEFAULT_DEVICE
    private var modelName = ModelConfig.EXTRACTOR_DEFAULT_MODEL

    private val _extractor: TfliteExtractor by lazy { TfliteExtractor(context, modelName, device) }

    fun extractImage(bitmap: Bitmap): FloatArray {
        return _extractor.extractImage(bitmap)
    }

    override fun close() {
        _extractor.close()
    }
}