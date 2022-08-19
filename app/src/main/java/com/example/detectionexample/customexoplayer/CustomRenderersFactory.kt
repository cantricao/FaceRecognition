package com.example.detectionexample.customexoplayer

import android.content.Context
import android.media.MediaFormat
import android.os.Handler
import android.util.Log
import androidx.media3.common.Format
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import java.nio.ByteBuffer

class CustomRenderersFactory(context: Context) :
    DefaultRenderersFactory(context) {
    private var videoFrameDataListener: VideoFrameDataListener? = null

    fun setVideoFrameDataListener(videoFrameDataListener: VideoFrameDataListener?): CustomRenderersFactory {
        this.videoFrameDataListener = videoFrameDataListener
        return this
    }

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        val videoRenderer = CustomMediaCodecVideoRenderer(
            context,
            mediaCodecSelector,
            allowedVideoJoiningTimeMs,
            enableDecoderFallback,
            eventHandler,
            eventListener,
            MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
        )
        forceDisableMediaCodecAsynchronousQueueing()
        experimentalSetSynchronizeCodecInteractionsWithQueueingEnabled(false)
        out.add(videoRenderer)
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return
        }
        var extensionRendererIndex = out.size
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            val clazz = Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer")
            val constructor = clazz.getConstructor(
                Long::class.javaPrimitiveType,
                Handler::class.java,
                VideoRendererEventListener::class.java,
                Int::class.javaPrimitiveType
            )
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            val renderer: Renderer = constructor.newInstance(
                allowedVideoJoiningTimeMs,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
            ) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded LibvpxVideoRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating VP9 extension", e)
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            val clazz = Class.forName("com.google.android.exoplayer2.ext.av1.Libgav1VideoRenderer")
            val constructor = clazz.getConstructor(
                Long::class.javaPrimitiveType,
                Handler::class.java,
                VideoRendererEventListener::class.java,
                Int::class.javaPrimitiveType
            )
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            val renderer: Renderer = constructor.newInstance(
                allowedVideoJoiningTimeMs,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
            ) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded Libgav1VideoRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating AV1 extension", e)
        }
    }

    private inner class CustomMediaCodecVideoRenderer(
        context: Context,
        mediaCodecSelector: MediaCodecSelector,
        allowedJoiningTimeMs: Long,
        enableDecoderFallback: Boolean,
        eventHandler: Handler?,
        eventListener: VideoRendererEventListener?,
        maxDroppedFramesToNotify: Int
    ) :
        MediaCodecVideoRenderer(
            context,
            CustomMediaCodecAdapter.Factory(),
            mediaCodecSelector,
            allowedJoiningTimeMs,
            enableDecoderFallback,
            eventHandler,
            eventListener,
            maxDroppedFramesToNotify
        ) {
        @Throws(ExoPlaybackException::class)
        override fun processOutputBuffer(
            positionUs: Long,
            elapsedRealtimeUs: Long,
            codec: MediaCodecAdapter?,
            buffer: ByteBuffer?,
            bufferIndex: Int,
            bufferFlags: Int,
            sampleCount: Int,
            bufferPresentationTimeUs: Long,
            isDecodeOnlyBuffer: Boolean,
            isLastBuffer: Boolean,
            format: Format
        ): Boolean {
            codec?.let { videoFrameDataListener?.onFrame(buffer, it.outputFormat, format) }
            return super.processOutputBuffer(
                positionUs,
                elapsedRealtimeUs,
                codec,
                buffer,
                bufferIndex,
                bufferFlags,
                sampleCount,
                bufferPresentationTimeUs,
                isDecodeOnlyBuffer,
                isLastBuffer,
                format
            )
        }
    }

    interface VideoFrameDataListener {
        fun onFrame(
            data: ByteBuffer?,
            androidMediaFormat: MediaFormat,
            playerFormat: Format
        )
    }

    companion object {
        private val TAG = CustomRenderersFactory::class.java.name
    }
}