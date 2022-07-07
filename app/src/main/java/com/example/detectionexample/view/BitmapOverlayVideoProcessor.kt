package com.example.detectionexample.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES31
import android.opengl.GLUtils
import com.example.detectionexample.models.BitmapProxy
import com.example.detectionexample.view.VideoProcessingGLSurfaceView.VideoProcessor
import com.google.android.exoplayer2.util.GlProgram
import com.google.android.exoplayer2.util.GlUtil
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10


/**
 * Video processor that demonstrates how to overlay a bitmap on video output using a GL shader. The
 * bitmap is drawn using an Android [Canvas].
 */
/* package */
class BitmapOverlayVideoProcessor(context: Context, private val analyzer: Analyzer) :
    VideoProcessor {
    private var widthBitmap: Int = 0
    private var heightBitmap: Int = 0
    private val context: Context
    private val paint: Paint
    private val textures: IntArray
    private val overlayBitmap: Bitmap
    private val overlayCanvas: Canvas
    private var program: @MonotonicNonNull GlProgram ? = null
    private var bitmapScaleX = 0f
    private var bitmapScaleY = 0f
    private var mPixelBuf: ByteBuffer? = null

    interface Analyzer {
        fun analyze(image: BitmapProxy)
    }


    override fun initialize() {

        program = try {
            GlProgram (
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
            "aTexCoords", GlUtil.getTextureCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
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
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,  /* level= */0, overlayBitmap,  /* border= */0)
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        bitmapScaleX = width.toFloat() / OVERLAY_WIDTH
        bitmapScaleY = height.toFloat() / OVERLAY_HEIGHT
        widthBitmap = width
        heightBitmap = height
        mPixelBuf = ByteBuffer.allocateDirect(widthBitmap * heightBitmap * Float.SIZE_BYTES).apply {
            order(ByteOrder.LITTLE_ENDIAN)
        }
    }

    override fun draw(frameTexture: Int, frameTimestampUs: Long, transformMatrix: FloatArray?) {
        // Draw to the canvas and store it in a texture.
//        val text =
//            String.format(Locale.US, "%.02f", frameTimestampUs / C.MICROS_PER_SECOND.toFloat())
//        overlayBitmap.eraseColor(Color.TRANSPARENT)
//        overlayCanvas.drawBitmap(logoBitmap!!, 32f, 32f, paint)
//        overlayCanvas.drawText(text, 200f, 130f, paint)
//        GLES31.glBindTexture(GL10.GL_TEXTURE_2D, textures[0])
//        GLUtils.texSubImage2D(
//            GL10.GL_TEXTURE_2D,  /* level= */0,  /* xoffset= */0,  /* yoffset= */0, overlayBitmap
//        )
//        GlUtil.checkGlError()

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


        mPixelBuf?.rewind()
        GLES31.glReadPixels(0, 0,
            widthBitmap, heightBitmap, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE,
            mPixelBuf)
        mPixelBuf?.rewind()
        val bitmapProxy = BitmapProxy(mPixelBuf!!, width = widthBitmap, height = heightBitmap, rotationDegrees = 180, frameTimestampUs, flipY = true)
        analyzer.analyze(bitmapProxy)
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
    }
}