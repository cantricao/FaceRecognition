package com.example.detectionexample.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES31
import android.opengl.GLUtils
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.PixelCopy.OnPixelCopyFinishedListener
import android.view.Surface
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.example.detectionexample.models.BitmapProxy
import com.example.detectionexample.view.VideoProcessingGLSurfaceView.VideoProcessor
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.util.GlProgram
import com.google.android.exoplayer2.util.GlUtil
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import java.io.IOException
import java.nio.IntBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import javax.microedition.khronos.opengles.GL10


/**
 * Video processor that demonstrates how to overlay a bitmap on video output using a GL shader. The
 * bitmap is drawn using an Android [Canvas].
 */
/* package */
class BitmapOverlayVideoProcessor(
    private val context: Context,
    private val analyzer: Analyzer,
    private val analysisExecutor: ExecutorService
) :
    VideoProcessor {
    private var mImageWidth: Int = 0
    private var mImageHeight: Int = 0
    private val paint: Paint = Paint()
    private val textures: IntArray
    private lateinit var overlayBitmap: Bitmap
    private lateinit var overlayCanvas: Canvas
    private var program: @MonotonicNonNull GlProgram? = null
    private var bitmapScaleX = 0f
    private var bitmapScaleY = 0f

    private lateinit var bitmapBuffer: IntArray
    private lateinit var bitmapSource: IntArray
    private lateinit var mPixelSource: IntBuffer
    private lateinit var mPixelBuf: IntBuffer
    @RequiresApi(Build.VERSION_CODES.N)
    private var copyHelper = SynchronousPixelCopy()
    private var dest: Bitmap? = null

    interface Analyzer {
        fun analyze(image: Bitmap, timestamp: Long)
    }


    class SynchronousPixelCopy {
        private val sHandler: Handler

        init {
            val thread = HandlerThread("PixelCopyHelper")
            thread.start()
            sHandler = Handler(thread.looper)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun request(source: Surface?, dest: Bitmap?, listener: OnPixelCopyFinishedListener) {
            PixelCopy.request(
                source!!,
                dest!!,
                listener,
                sHandler
            )
        }
    }


    override fun initialize() {

        program = try {
            GlProgram(
                context.applicationContext,  /* vertexShaderFilePath= */
                "bitmap_overlay_video_processor_vertex.glsl",
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
        GLES31.glGenTextures(1, textures, 0)
        GLES31.glBindTexture(GL10.GL_TEXTURE_2D, textures[0])
        GLES31.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_NEAREST.toFloat()
        )
        GLES31.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES31.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT.toFloat())
        GLES31.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT.toFloat())
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        bitmapScaleX = width.toFloat() / OVERLAY_WIDTH
        bitmapScaleY = height.toFloat() / OVERLAY_HEIGHT
        mImageWidth = width
        mImageHeight = height
        bitmapBuffer = IntArray(mImageWidth * mImageHeight)
        bitmapSource = IntArray(mImageWidth * mImageHeight)
        mPixelBuf = IntBuffer.wrap(bitmapBuffer)
        mPixelSource = IntBuffer.wrap(bitmapSource)
        overlayBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888)
        overlayCanvas = Canvas(overlayBitmap)
        dest = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888)
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,  /* level= */0, overlayBitmap,  /* border= */0)
    }


    override fun draw(
        frameTexture: Int,
        frameTimestampUs: Long,
        transformMatrix: FloatArray?,
        surface: Surface?
    ) {
        // Draw to the canvas and store it in a texture.
        val text =
            String.format(Locale.US, "%.02f", frameTimestampUs / C.MICROS_PER_SECOND.toFloat())
        overlayBitmap.eraseColor(Color.TRANSPARENT)
        overlayCanvas.drawText(text, 200f, 130f, paint)
        GLES31.glBindTexture(GL10.GL_TEXTURE_2D, textures[0])
        GLUtils.texSubImage2D(
            GL10.GL_TEXTURE_2D,  /* level= */0,  /* xoffset= */0,  /* yoffset= */0, overlayBitmap
        )
        GlUtil.checkGlError()

        // Run the shader program.
        val program = checkNotNull(program)
        program.setSamplerTexIdUniform("uTexSampler0", frameTexture,  /* unit= */0)
        program.setSamplerTexIdUniform("uTexSampler1", textures[0],  /* unit= */1)
        program.setFloatUniform("uScaleX", bitmapScaleX)
        program.setFloatUniform("uScaleY", bitmapScaleY)
        program.setFloatsUniform("uTexTransform", transformMatrix!!)
        program.bindAttributesAndUniforms()
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP,  /* first= */0,  /* count= */4)
        GlUtil.checkGlError()

        mPixelBuf.rewind()
        analyzerFrame(surface, frameTimestampUs)
    }

    private fun analyzerFrame(surface: Surface?, frameTimestampUs: Long) {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) && (surface != null)) {
            val listener = OnPixelCopyFinishedListener {
                analyzer.analyze(dest!!, frameTimestampUs)
            }
            copyHelper.request(surface, dest, listener)
        } else {
            GLES31.glReadPixels(
                0, 0,
                mImageWidth, mImageHeight, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE,
                mPixelBuf
            )
            mPixelBuf.rewind()
            for (i in 0 until mImageHeight) {
                for (j in 0 until mImageWidth) {
                    bitmapSource[(mImageHeight - i - 1) * mImageWidth + j] =
                        bitmapBuffer[i * mImageWidth + j]
                }
            }
            dest?.setPixels(bitmapSource, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight)
            analyzer.analyze(dest!!, frameTimestampUs)
        }
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
        paint.textSize = 64f
        paint.isAntiAlias = true
        paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF)
        textures = IntArray(1)
    }
}