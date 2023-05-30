package com.example.detectionexample.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import androidx.compose.runtime.State
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Assertions.checkStateNotNull
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Util
import com.example.detectionexample.models.TrackedRecognition
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.opengles.GL10


/**
 * Video processor that demonstrates how to overlay a bitmap on video output using a GL shader. The
 * bitmap is drawn using an Android [Canvas].
 */
/* package */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class BitmapOverlayVideoProcessor(
    context: Context,
    private val trackedObserver: State<List<TrackedRecognition>>
) :
    VideoProcessingGLSurfaceView.VideoProcessor {
    private val context: Context
    private val paint: Paint
    private val textures: IntArray
    private lateinit var overlayBitmap: Bitmap
    private lateinit var overlayCanvas: Canvas
    private var program: @MonotonicNonNull GlProgram? = null
    private var bitmapScaleX = 1f
    private var bitmapScaleY = 1f
//    private lateinit var mPixelBuf : ByteBuffer
//    private lateinit var imageBitmap: Bitmap
    private lateinit var detectionDrawable: DetectionDrawable

    private var singleThreadExecutorService: ExecutorService? = null

    interface BitmapAnalyzer {
        fun analyze(image: Bitmap, timestamp: Long)
    }

    override fun initialize() {
        program = try {
            GlProgram(
                context,  /* vertexShaderFilePath = */
                "bitmap_overlay_video_processor_vertex.glsl",  /* fragmentShaderFilePath = */
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

        singleThreadExecutorService = Util.newSingleThreadExecutor(THREAD_NAME)


    }

    override fun setSurfaceSize(width: Int, height: Int) {
        bitmapScaleX = 1f
        bitmapScaleY = 1f
//        mPixelBuf =  ByteBuffer.allocate(width * height * 4)
//        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN)
//        imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        overlayBitmap = Bitmap.createBitmap(OVERLAY_WIDTH, OVERLAY_HEIGHT, Bitmap.Config.ARGB_8888)
        overlayCanvas = Canvas(overlayBitmap)
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,  /* level = */0, overlayBitmap,  /* border = */0)
        detectionDrawable = DetectionDrawable(trackedObserver, overlayCanvas.width, overlayCanvas.height)

    }



    override fun draw(frameTexture: Int, frameTimestampUs: Long, transformMatrix: FloatArray?) {
        // Draw to the canvas and store it in a texture.
        overlayBitmap.eraseColor(Color.TRANSPARENT)
        detectionDrawable.draw(overlayCanvas)
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0])
        GLUtils.texSubImage2D(
            GL10.GL_TEXTURE_2D,  /* level = */0,  /* xoffset = */0,  /* yoffset = */0, overlayBitmap
        )
        GlUtil.checkGlError()

        // Run the shader program.
        val program = Assertions.checkNotNull(program)
        program.setSamplerTexIdUniform("uTexSampler0", frameTexture,  /* texUnitIndex = */0)
        program.setSamplerTexIdUniform("uTexSampler1", textures[0],  /* texUnitIndex = */1)
        program.setFloatUniform("uScaleX", bitmapScaleX)
        program.setFloatUniform("uScaleY", bitmapScaleY)
        program.setFloatsUniform("uTexTransform", transformMatrix!!)
        program.bindAttributesAndUniforms()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,  /* first = */0,  /* count = */4)
        GlUtil.checkGlError()


//        mPixelBuf.rewind()
//        GLES20.glReadPixels(0, 0, imageBitmap.width, imageBitmap.height, GLES20.GL_RGBA,
//            GLES20.GL_UNSIGNED_BYTE, mPixelBuf)
//        GlUtil.checkGlError()
//        mPixelBuf.rewind()
//        imageBitmap.copyPixelsFromBuffer(mPixelBuf)
//        bitmapAnalyzer.analyze(Bitmap.createBitmap(imageBitmap), frameTimestampUs)
    }


    override fun release() {
        if (program != null) {
            program!!.delete()
        }

        val singleThreadExecutorService = checkStateNotNull(singleThreadExecutorService)
        singleThreadExecutorService.shutdown()
        try {
            if (!singleThreadExecutorService.awaitTermination(RELEASE_WAIT_TIME_MS, TimeUnit.MILLISECONDS
                )) {
                Log.e(TAG, "error: Release timed out")

            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(TAG, "error: ${e.message}")
        }

    }


    companion object {
        private const val TAG = "BitmapOverlayVideoProcessor"
        private const val OVERLAY_WIDTH = 512
        private const val OVERLAY_HEIGHT = 256
        private const val THREAD_NAME = "BitmapOverlayVideoProcessor"
        private const val RELEASE_WAIT_TIME_MS: Long = 100
    }

    init {
        this.context = context.applicationContext
        paint = Paint()
        paint.textSize = 64f
        paint.isAntiAlias = true
        paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF)
        textures = IntArray(1)
    }
}