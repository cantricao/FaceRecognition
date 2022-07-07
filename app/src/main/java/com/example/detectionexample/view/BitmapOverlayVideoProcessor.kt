package com.example.detectionexample.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES31
import android.opengl.GLUtils
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
    context: Context,
    private val analyzer: Analyzer,
    private val analysisExecutor: ExecutorService
) :
    VideoProcessor {
    private var mImageWidth: Int = 0
    private var mImageHeight: Int = 0
    private val context: Context
    private val paint: Paint
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

    interface Analyzer {
        fun analyze(image: BitmapProxy)
    }


    override fun initialize() {

        program = try {
            GlProgram(
                context,  /* vertexShaderFilePath= */
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
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,  /* level= */0, overlayBitmap,  /* border= */0)
    }


    override fun draw(frameTexture: Int, frameTimestampUs: Long, transformMatrix: FloatArray?) {
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
        GLES31.glReadPixels(
            0, 0,
            mImageWidth, mImageHeight, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE,
            mPixelBuf
        )
        mPixelBuf.rewind()

        analysisExecutor.execute {
            for (i in 0 until mImageHeight) {
                for (j in 0 until mImageWidth) {
                    bitmapSource[(mImageHeight - i - 1) * mImageWidth + j] = bitmapBuffer[i * mImageWidth + j]
                }
            }
            mPixelSource.rewind()
            val bitmapProxy = BitmapProxy(
                mPixelSource,
                width = mImageWidth,
                height = mImageHeight,
                rotationDegrees = 0,
                frameTimestampUs,
                flipY = false
            )
            analyzer.analyze(bitmapProxy)
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
        this.context = context.applicationContext
        paint = Paint()
        paint.textSize = 64f
        paint.isAntiAlias = true
        paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF)
        textures = IntArray(1)
    }
}