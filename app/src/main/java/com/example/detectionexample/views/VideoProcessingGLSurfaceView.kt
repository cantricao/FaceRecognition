package com.example.detectionexample.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.compose.runtime.State
import androidx.core.util.Preconditions
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.GlUtil.GlException
import androidx.media3.common.util.Log
import androidx.media3.common.util.TimedValueQueue
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import com.example.detectionexample.config.Util
import com.example.detectionexample.grafika.FullFrameRect
import com.example.detectionexample.grafika.Texture2dProgram
import com.example.detectionexample.grafika.TextureMovieEncoder
import com.example.detectionexample.uistate.RecordState
import com.example.detectionexample.views.VideoProcessingGLSurfaceView.VideoProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

/**
 * [GLSurfaceView] that creates a GL context (optionally for protected content) and passes
 * video frames to a [VideoProcessor] for drawing to the view.
 *
 *
 * This view must be created programmatically, as it is necessary to specify whether a context
 * supporting protected content should be created at construction time.
 *
 * Creates a new instance. Pass `true` for `requireSecureContext` if the [ ] associated GL context should handle secure content (if the
 * device supports it).
 *
 * @param context The [Context].
 * @param requireSecureContext Whether a GL context supporting protected content should be
 * created, if supported by the device.
 * @param videoProcessor Processor that draws to the view.
*/


@SuppressLint("ViewConstructor")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoProcessingGLSurfaceView(
    context: Context?, requireSecureContext: Boolean, videoProcessor: VideoProcessor?,
    mVideoEncoder: TextureMovieEncoder, mRecordingEnabled: State<RecordState>,
    bitmapAnalyzer: BitmapOverlayVideoProcessor.BitmapAnalyzer
) : GLSurfaceView(context) {
    /** Processes video frames, provided via a GL texture.  */
    interface VideoProcessor {
        /** Performs any required GL initialization.  */
        fun initialize()

        /** Sets the size of the output surface in pixels.  */
        fun setSurfaceSize(width: Int, height: Int)

        /**
         * Draws using GL operations.
         *
         * @param frameTexture The ID of a GL texture containing a video frame.
         * @param frameTimestampUs The presentation timestamp of the frame, in microseconds.
         * @param transformMatrix The 4 * 4 transform matrix to be applied to the texture.
         */
        fun draw(frameTexture: Int, frameTimestampUs: Long, transformMatrix: FloatArray?)

        /** Releases any resources associated with this [VideoProcessor].  */
        fun release()
    }

    private val renderer: VideoRenderer
    private val mainHandler: Handler

    private var surfaceTexture: SurfaceTexture? = null

    private var surface: Surface? = null

    private var player: ExoPlayer? = null


    init {
        renderer = VideoRenderer(videoProcessor, mVideoEncoder, mRecordingEnabled, bitmapAnalyzer)
        mainHandler = Handler(Looper.getMainLooper())
        setEGLContextClientVersion(2)
        setEGLConfigChooser( /* redSize = */
            8,  /* greenSize = */
            8,  /* blueSize = */
            8,  /* alphaSize = */
            8,  /* depthSize = */
            0,  /* stencilSize = */
            0
        )
        setEGLContextFactory(
            object : EGLContextFactory {
                override fun createContext(
                    egl: EGL10,
                    display: EGLDisplay,
                    eglConfig: EGLConfig
                ): EGLContext {
                    val glAttributes: IntArray = if (requireSecureContext) {
                        intArrayOf(
                            EGL14.EGL_CONTEXT_CLIENT_VERSION,
                            2,
                            EGL_PROTECTED_CONTENT_EXT,
                            EGL14.EGL_TRUE,
                            EGL14.EGL_NONE
                        )
                    } else {
                        intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                    }
                    return egl.eglCreateContext(
                        display, eglConfig,  /* share_context = */EGL10.EGL_NO_CONTEXT, glAttributes
                    )
                }

                override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
                    egl.eglDestroyContext(display, context)
                }
            })
        setEGLWindowSurfaceFactory(
            object : EGLWindowSurfaceFactory {
                override fun createWindowSurface(
                    egl: EGL10, display: EGLDisplay, config: EGLConfig, nativeWindow: Any
                ): EGLSurface {
                    val attribsList = if (requireSecureContext) intArrayOf(
                        EGL_PROTECTED_CONTENT_EXT,
                        EGL14.EGL_TRUE,
                        EGL10.EGL_NONE
                    ) else intArrayOf(EGL10.EGL_NONE)
                    return egl.eglCreateWindowSurface(display, config, nativeWindow, attribsList)
                }

                override fun destroySurface(egl: EGL10, display: EGLDisplay, surface: EGLSurface) {
                    egl.eglDestroySurface(display, surface)
                }
            })
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

    }

    /**
     * Attaches or detaches (if `player` is `null`) this view from the player.
     *
     * @param player The new player, or `null` to detach this view.
     */
    fun setPlayer(player: ExoPlayer?) {
        if (player === this.player) {
            return
        }
        if (this.player != null) {
            if (surface != null) {
                this.player!!.clearVideoSurface(surface)
            }
            this.player!!.clearVideoFrameMetadataListener(renderer)
        }
        this.player = player
        if (this.player != null) {
            this.player!!.setVideoFrameMetadataListener(renderer)
            this.player!!.setVideoSurface(surface)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Post to make sure we occur in order with any onSurfaceTextureAvailable calls.
        mainHandler.post {
            if (surface != null) {
                if (player != null) {
                    player!!.setVideoSurface(null)
                }
                releaseSurface(surfaceTexture, surface)
                surfaceTexture = null
                renderer.notifyPausing()
                surface = null
            }
        }
    }
    private fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture) {
        mainHandler.post {
            val oldSurfaceTexture = this.surfaceTexture
            val oldSurface = surface
            this.surfaceTexture = surfaceTexture
            surface = Surface(surfaceTexture)
            releaseSurface(oldSurfaceTexture, oldSurface)
            if (player != null) {
                player!!.setVideoSurface(surface)
            }
        }
    }

    private inner class VideoRenderer(private val videoProcessor: VideoProcessor?,
                                      private val mVideoEncoder: TextureMovieEncoder,
                                      private val mRecordingEnabled: State<RecordState>,
                                      private val bitmapAnalyzer: BitmapOverlayVideoProcessor.BitmapAnalyzer) :
        Renderer, VideoFrameMetadataListener {
        private val frameAvailable: AtomicBoolean = AtomicBoolean()
        private val sampleTimestampQueue: TimedValueQueue<Long> = TimedValueQueue()
        private val transformMatrix: FloatArray
        private var texture = 0

        private var surfaceTexture: SurfaceTexture? = null
        private var initialized = false
        private var width: Int
        private var height: Int
        private var frameTimestampUs: Long

        private var mFullScreen: FullFrameRect? = null

        private var mRecordingStatus = 0
        private var mFrameCount = 0

        // width/height of the incoming camera preview frames
        private var mIncomingSizeUpdated = false
        private var mIncomingWidth = 0
        private var mIncomingHeight = 0
        private var mFrameRate = 0

        private lateinit var mPixelBuf : ByteBuffer
        private lateinit var imageBitmap: Bitmap

        init {
            width = -1
            height = -1
            frameTimestampUs = C.TIME_UNSET
            transformMatrix = FloatArray(16)

            mRecordingStatus = -1
            mFrameCount = -1

            mIncomingSizeUpdated = false
            mIncomingWidth = (-1).also { mIncomingHeight = it }
        }

        /**
         * Notifies the renderer thread that the activity is pausing.
         *
         *
         * For best results, call this *after* disabling Camera preview.
         */
        fun notifyPausing() {
            if (surfaceTexture != null) {
                android.util.Log.d(
                    TAG,
                    "renderer pausing -- releasing SurfaceTexture"
                )
                surfaceTexture!!.release()
                surfaceTexture = null
            }
            if (mFullScreen != null) {
                mFullScreen!!.release(false) // assume the GLSurfaceView EGL context is about
                mFullScreen = null //  to be destroyed
            }
            mIncomingHeight = -1
            mIncomingWidth = mIncomingHeight
        }


        /**
         * Updates the filter program.
         */
        fun updateFilter() {
            val kernel: FloatArray? = null
            val colorAdj = 0.0f
            val programType: Texture2dProgram.ProgramType = Texture2dProgram.ProgramType.TEXTURE_EXT

            // Do we need a whole new program?  (We want to avoid doing this if we don't have
            // too -- compiling a program could be expensive.)
            if (programType !== mFullScreen!!.program.programType) {
                mFullScreen!!.changeProgram(Texture2dProgram(programType))
                // If we created a new program, we need to initialize the texture width/height.
                mIncomingSizeUpdated = true
            }

            // Update the filter kernel (if any).
            if (kernel != null) {
                mFullScreen!!.program.setKernel(kernel, colorAdj)
            }
        }

        @Synchronized
        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            try {
                texture = GlUtil.createExternalTexture()
            } catch (e: GlException) {
                Log.e(TAG, "Failed to create an external texture", e)
            }
            surfaceTexture = SurfaceTexture(texture)
            surfaceTexture!!.setOnFrameAvailableListener {
                frameAvailable.set(true)
                requestRender()
            }
            onSurfaceTextureAvailable(surfaceTexture!!)


            Log.d(TAG, "onSurfaceCreated")

            // We're starting up or coming back.  Either way we've got a new EGLContext that will
            // need to be shared with the video encoder, so figure out if a recording is already
            // in progress.
//            mRecordingEnabled.value = mVideoEncoder.isRecording
            mRecordingStatus = if (mRecordingEnabled.value == RecordState.RECORDING) {
                RECORDING_RESUMED
            } else {
                RECORDING_OFF
            }

        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            this.width = width
            this.height = height

        }

        /**
         * Takes a snapshot of the current external texture and returns a Bitmap.
         *
         * @param size             the size of the output [Bitmap].
         * @param textureTransform the transformation matrix.
         * See: [SurfaceOutput.updateTransformMatrix]
         */
        fun snapshot(
            size: Size,
            textureTransform: FloatArray,
            mExternalTextureId: Int,
            mTexMatrixLoc: Int
        ): Bitmap {
            // Allocate buffer.
            val byteBuffer = ByteBuffer.allocateDirect(
                size.width * size.height * PIXEL_STRIDE
            )
            // Take a snapshot.
            snapshot(byteBuffer, size, textureTransform, mExternalTextureId, mTexMatrixLoc)
            // Create a Bitmap and copy the bytes over.
            val bitmap = Bitmap.createBitmap(
                size.width, size.height, Bitmap.Config.ARGB_8888
            )
            byteBuffer.rewind()
            bitmap.copyPixelsFromBuffer(byteBuffer)
            return bitmap
        }

        /**
         * Takes a snapshot of the current external texture and stores it in the given byte buffer.
         *
         *
         *  The image is stored as RGBA with pixel stride of 4 bytes and row stride of width * 4
         * bytes.
         *
         * @param byteBuffer       the byte buffer to store the snapshot.
         * @param size             the size of the output image.
         * @param textureTransform the transformation matrix.
         * See: [SurfaceOutput.updateTransformMatrix]
         */
        @SuppressLint("RestrictedApi")
        private fun snapshot(
            byteBuffer: ByteBuffer, size: Size,
            textureTransform: FloatArray,
            mExternalTextureId: Int,
            mTexMatrixLoc: Int
        ) {
            Preconditions.checkArgument(
                byteBuffer.capacity() == size.width * size.height * 4,
                "ByteBuffer capacity is not equal to width * height * 4."
            )
            Preconditions.checkArgument(byteBuffer.isDirect, "ByteBuffer is not direct.")

            // Create and initialize intermediate texture.
            val texture = generateTexture()
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GlUtil.checkGlError()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GlUtil.checkGlError()
            // Configure the texture.
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, size.width,
                size.height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null
            )
            GlUtil.checkGlError()
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
            )

            // Create FBO.
            val fbo = generateFbo()
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
            GlUtil.checkGlError()

            // Attach the intermediate texture to the FBO
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0
            )
            GlUtil.checkGlError()

            // Bind external texture (camera output).
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GlUtil.checkGlError()
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId)
            GlUtil.checkGlError()

            // Set scissor and viewport.
            GLES20.glViewport(0, 0, size.width, size.height)
            GLES20.glScissor(0, 0, size.width, size.height)

            // Upload transform matrix.
            GLES20.glUniformMatrix4fv(
                mTexMatrixLoc,  /*count=*/1,  /*transpose=*/false, textureTransform,  /*offset=*/
                0
            )
            GlUtil.checkGlError()

            // Draw the external texture to the intermediate texture.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,  /*firstVertex=*/0,  /*vertexCount=*/4)
            GlUtil.checkGlError()

            // Read the pixels from the framebuffer
            GLES20.glReadPixels(
                0, 0, size.width, size.height, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                byteBuffer
            )
            GlUtil.checkGlError()

            // Clean up
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            deleteTexture(texture)
            deleteFbo(fbo)
            // Set the external texture to be active.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId)
        }


        private fun generateFbo(): Int {
            val fbos = IntArray(1)
            GLES20.glGenFramebuffers(1, fbos, 0)
            GlUtil.checkGlError()
            return fbos[0]
        }

        private fun generateTexture(): Int {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            GlUtil.checkGlError()
            return textures[0]
        }

        private fun deleteTexture(texture: Int) {
            val textures = intArrayOf(texture)
            GLES20.glDeleteTextures(1, textures, 0)
            GlUtil.checkGlError()
        }

        private fun deleteFbo(fbo: Int) {
            val fbos = intArrayOf(fbo)
            GLES20.glDeleteFramebuffers(1, fbos, 0)
            GlUtil.checkGlError()
        }

        override fun  onDrawFrame(gl: GL10) {
            if (videoProcessor == null) {
                return
            }
            if (!initialized) {
                videoProcessor.initialize()
                initialized = true
            }
            if (width != -1 && height != -1) {
                videoProcessor.setSurfaceSize(width, height)
                imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                mPixelBuf =  ByteBuffer.allocate(width * height * 4)
                mPixelBuf.order(ByteOrder.LITTLE_ENDIAN)

                width = -1
                height = -1
            }

            // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
            // was there before.
            if (frameAvailable.compareAndSet(true, false)) {
                val surfaceTexture = Assertions.checkNotNull(this.surfaceTexture)
                surfaceTexture.updateTexImage()
                val lastFrameTimestampNs = surfaceTexture.timestamp
                val frameTimestampUs = sampleTimestampQueue.poll(lastFrameTimestampNs)
                if (frameTimestampUs != null) {
                    this.frameTimestampUs = frameTimestampUs
                }
                surfaceTexture.getTransformMatrix(transformMatrix)
            }
            videoProcessor.draw(texture, frameTimestampUs, transformMatrix)


            mPixelBuf.rewind()
            GLES20.glReadPixels(0, 0, imageBitmap.width, imageBitmap.height, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, mPixelBuf)
            GlUtil.checkGlError()
            mPixelBuf.rewind()
            imageBitmap.copyPixelsFromBuffer(mPixelBuf)
            bitmapAnalyzer.analyze(Bitmap.createBitmap(imageBitmap), frameTimestampUs)


            if (VERBOSE) android.util.Log.d(
                TAG,
                "onDrawFrame tex=$texture"
            )


            // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
            // was there before.
            surfaceTexture?.updateTexImage()

            // If the recording state is changing, take care of it here.  Ideally we wouldn't
            // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
            // makes it hard to do elsewhere.
            if (mRecordingEnabled.value == RecordState.RECORDING) {
                when (mRecordingStatus) {
                    RECORDING_OFF -> {
                        val outputFile = Util.createFile("mp4")
                        Log.d(TAG, "START recording:$outputFile")
                        // start recording
                        mVideoEncoder.startRecording(
                            TextureMovieEncoder.EncoderConfig(
                                outputFile,
                                mIncomingWidth,
                                mIncomingHeight,
                                mFrameRate,
                                10000000,
                                EGL14.eglGetCurrentContext()
                            )
                        )
                        mRecordingStatus = RECORDING_ON
                    }
                    RECORDING_RESUMED -> {
                        android.util.Log.d(
                            TAG,
                            "RESUME recording"
                        )
                        mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext())
                        mRecordingStatus = RECORDING_ON
                    }
                    RECORDING_ON -> {}
                    else -> throw RuntimeException("unknown status $mRecordingStatus")
                }
            } else {
                when (mRecordingStatus) {
                    RECORDING_ON, RECORDING_RESUMED -> {
                        // stop recording
                        android.util.Log.d(
                            TAG,
                            "STOP recording"
                        )
                        mVideoEncoder.stopRecording()

                        mRecordingStatus =
                            RECORDING_OFF
                    }
                    RECORDING_OFF -> {}
                    else -> throw RuntimeException("unknown status $mRecordingStatus")
                }
            }

            // Set the video encoder's texture name.  We only need to do this once, but in the
            // current implementation it has to happen after the video encoder is started, so
            // we just do it here.
            // TODO: be less lame.
            mVideoEncoder.setTextureId(texture)

            // Tell the video encoder thread that a new frame is available.
            // This will be ignored if we're not actually recording.
            mVideoEncoder.frameAvailable(surfaceTexture)

            if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
                // Texture size isn't set yet.  This is only used for the filters, but to be
                // safe we can just skip drawing while we wait for the various races to resolve.
                // (This seems to happen if you toggle the screen off/on with power button.)
                Log.i(TAG, "Drawing before incoming texture size set; skipping")
                return
            }

            // Draw a flashing box if we're recording.  This only appears on screen.
            val showBox: Boolean = mRecordingStatus == RECORDING_ON
            if (showBox && ++mFrameCount and 0x04 == 0) {
                 drawBox()
            }

        }

        /**
         * Draws a red box in the corner.
         */
        private fun drawBox() {
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            GLES20.glScissor(0, 0, 100, 100)
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        }

        override fun onVideoFrameAboutToBeRendered(
            presentationTimeUs: Long,
            releaseTimeNs: Long,
            format: Format,
            mediaFormat: MediaFormat?
        ) {
            sampleTimestampQueue.add(releaseTimeNs, presentationTimeUs)

            mIncomingWidth = format.width
            mIncomingHeight = format.height
            mIncomingSizeUpdated = true
            mFrameRate = format.frameRate.toInt()
        }
    }

    companion object {
        private const val EGL_PROTECTED_CONTENT_EXT = 0x32C0
        private const val TAG = "VPGlSurfaceView"
        private const val VERBOSE = false

        private const val RECORDING_OFF = 0
        private const val RECORDING_ON = 1
        private const val RECORDING_RESUMED = 2
        private const val PIXEL_STRIDE = 4
        private fun releaseSurface(
            oldSurfaceTexture: SurfaceTexture?, oldSurface: Surface?
        ) {
            oldSurfaceTexture?.release()
            oldSurface?.release()
        }
    }
}