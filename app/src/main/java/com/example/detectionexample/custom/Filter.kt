package com.example.detectionexample.custom

interface Filter {
    fun onDrawFrame(textureId: Int): Int
    fun setTransformMatrix(mtx: FloatArray)
    fun onReady(width: Int, height: Int)
}