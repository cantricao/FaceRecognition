/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.detectionexample.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.media3.common.C
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import com.example.detectionexample.custom.VideoProcessingGLSurfaceView.VideoProcessor
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import java.io.IOException
import java.util.*
import javax.microedition.khronos.opengles.GL10

/**
 * Video processor that demonstrates how to overlay a bitmap on video output using a GL shader. The
 * bitmap is drawn using an Android [Canvas].
 */
/* package */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class BitmapOverlayVideoProcessor(context: Context) : VideoProcessor {
    private val context: Context
    private val paint: Paint
    private val textures: IntArray
    private val overlayBitmap: Bitmap
    private var logoBitmap: Bitmap? = null
    private val overlayCanvas: Canvas
    private var program: @MonotonicNonNull GlProgram? = null
    private var bitmapScaleX = 0f
    private var bitmapScaleY = 0f
    override fun initialize() {
        program = try {
            GlProgram(
                context,  /* vertexShaderFilePath= */
                "bitmap_overlay_video_processor_vertex.glsl",  /* fragmentShaderFilePath= */
                "bitmap_overlay_video_processor_fragment.glsl"
            )
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
        program!!.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        program!!.setBufferAttribute(
            "aTexCoords",
            GlUtil.getTextureCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT.toFloat())
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT.toFloat())
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,  /* level= */0, overlayBitmap,  /* border= */0)
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        bitmapScaleX = width.toFloat() / OVERLAY_WIDTH
        bitmapScaleY = height.toFloat() / OVERLAY_HEIGHT
    }

    override fun draw(frameTexture: Int, frameTimestampUs: Long, transformMatrix: FloatArray?) {
        // Draw to the canvas and store it in a texture.
        val text =
            String.format(Locale.US, "%.02f", frameTimestampUs / C.MICROS_PER_SECOND.toFloat())
        overlayBitmap.eraseColor(Color.TRANSPARENT)
//        overlayCanvas.drawBitmap(logoBitmap!!, 32f, 32f, paint)
        overlayCanvas.drawText(text, 200f, 130f, paint)
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0])
        GLUtils.texSubImage2D(
            GL10.GL_TEXTURE_2D,  /* level= */0,  /* xoffset= */0,  /* yoffset= */0, overlayBitmap
        )
        GlUtil.checkGlError()

        // Run the shader program.
        val program = Assertions.checkNotNull(program)
        program.setSamplerTexIdUniform("uTexSampler0", frameTexture,  /* texUnitIndex= */0)
        program.setSamplerTexIdUniform("uTexSampler1", textures[0],  /* texUnitIndex= */1)
        program.setFloatUniform("uScaleX", bitmapScaleX)
        program.setFloatUniform("uScaleY", bitmapScaleY)
        program.setFloatsUniform("uTexTransform", transformMatrix!!)
        program.bindAttributesAndUniforms()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,  /* first= */0,  /* count= */4)
        GlUtil.checkGlError()
    }

    override fun release() {
        if (program != null) {
            program!!.delete()
        }
    }

    companion object {
        private const val OVERLAY_WIDTH = 512
        private const val OVERLAY_HEIGHT = 256
    }

    init {
        this.context = context.applicationContext
        paint = Paint()
        paint.textSize = 64f
        paint.isAntiAlias = true
        paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF)
        textures = IntArray(1)
        overlayBitmap = Bitmap.createBitmap(OVERLAY_WIDTH, OVERLAY_HEIGHT, Bitmap.Config.ARGB_8888)
        overlayCanvas = Canvas(overlayBitmap)
//        logoBitmap = try {
//            (context.packageManager.getApplicationIcon(context.packageName) as BitmapDrawable)
//                .bitmap
//        } catch (e: PackageManager.NameNotFoundException) {
//            throw IllegalStateException(e)
//        }
    }
}