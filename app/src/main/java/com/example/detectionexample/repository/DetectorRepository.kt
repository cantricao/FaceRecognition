package com.example.detectionexample.repository

import android.graphics.Bitmap
import javax.inject.Singleton

@Singleton
interface DetectorRepository {

    fun detectInImage(bitmap: Bitmap)


    fun setFileModelName(filename: String) {

    }

    fun setModelDevice(name: String) {

    }
}