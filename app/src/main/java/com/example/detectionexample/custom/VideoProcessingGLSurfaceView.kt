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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.TimedValueQueue
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import com.example.detectionexample.custom.VideoProcessingGLSurfaceView.VideoProcessor
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.*
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
    context: Context?, requireSecureContext: Boolean, videoProcessor: VideoProcessor?
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

    private val renderer: VideoRenderer = VideoRenderer(videoProcessor)
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var player: ExoPlayer? = null
    private val imageReader = ImageReader.newInstance(360, 480, ImageFormat.YUV_420_888, 1)

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
            surface = imageReader.surface
            releaseSurface(oldSurfaceTexture, oldSurface)
            if (player != null) {
                player!!.setVideoSurface(surface)
            }
        }
    }

    private inner class VideoRenderer(private val videoProcessor: VideoProcessor?) : Renderer,
        VideoFrameMetadataListener {
        private val frameAvailable: AtomicBoolean = AtomicBoolean()
        private val sampleTimestampQueue: TimedValueQueue<Long> = TimedValueQueue()
        private val transformMatrix: FloatArray = FloatArray(16)
        private var texture = 0
        private var surfaceTexture: SurfaceTexture? = null
        private var initialized = false
        private var width: Int = -1
        private var height: Int = -1
        private var frameTimestampUs: Long = C.TIME_UNSET
        @Synchronized
        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            texture = GlUtil.createExternalTexture()
            surfaceTexture = SurfaceTexture(texture)
            surfaceTexture!!.setOnFrameAvailableListener {
                frameAvailable.set(true)
                requestRender()
            }
            onSurfaceTextureAvailable(surfaceTexture!!)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            this.width = width
            this.height = height
        }

        override fun onDrawFrame(gl: GL10) {
            if (videoProcessor == null) {
                return
            }
            if (!initialized) {
                videoProcessor.initialize()
                initialized = true
            }
            if (width != -1 && height != -1) {
                videoProcessor.setSurfaceSize(width, height)
                width = -1
                height = -1
            }
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
        }

        override fun onVideoFrameAboutToBeRendered(
            presentationTimeUs: Long,
            releaseTimeNs: Long,
            format: Format,
            mediaFormat: MediaFormat?
        ) {
            sampleTimestampQueue.add(releaseTimeNs, presentationTimeUs)
        }

    }

    companion object {
        private const val EGL_PROTECTED_CONTENT_EXT = 0x32C0
        private fun releaseSurface(
            oldSurfaceTexture: SurfaceTexture?, oldSurface: Surface?
        ) {
            oldSurfaceTexture?.release()
            oldSurface?.release()
        }
    }

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser( /* redSize= */
            8,  /* greenSize= */
            8,  /* blueSize= */
            8,  /* alphaSize= */
            8,  /* depthSize= */
            0,  /* stencilSize= */
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
                        display, eglConfig,  /* share_context= */EGL10.EGL_NO_CONTEXT, glAttributes
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
}