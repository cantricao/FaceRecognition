package com.example.detectionexample.custom

import android.media.Image
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.util.TraceUtil
import androidx.media3.common.util.Util
import androidx.media3.common.util.Util.castNonNull
import androidx.media3.decoder.CryptoInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import java.io.IOException
import java.nio.ByteBuffer

/**
 * A [MediaCodecAdapter] that operates the underlying [MediaCodec] in synchronous mode.
 */
class ExoplayerCustomMediaCodecAdapter private constructor(private val codec: MediaCodec) :
    MediaCodecAdapter {
    /** A factory for {@link SynchronousMediaCodecAdapter} instances.  */
    class Factory : MediaCodecAdapter.Factory {

        @Throws(IOException::class)
        override fun createAdapter(configuration: MediaCodecAdapter.Configuration): MediaCodecAdapter {
            var codec: MediaCodec? = null

            return try {
                codec = createCodec(configuration)
                TraceUtil.beginSection("configureCodec")
                codec.configure(
                    configuration.mediaFormat,  /*configuration.surface*/
                    null, // null
                    configuration.crypto,
                    configuration.flags
                )
                TraceUtil.endSection()
                TraceUtil.beginSection("startCodec")
                codec.start()
                TraceUtil.endSection()
                ExoplayerCustomMediaCodecAdapter(codec)
            } catch (e: IOException) {
                codec?.release()
                throw e
            } catch (e: RuntimeException) {
                codec?.release()
                throw e
            }
        }

        /** Creates a new [MediaCodec] instance.  */
        @Throws(IOException::class)
        fun createCodec(configuration: MediaCodecAdapter.Configuration): MediaCodec {
            checkNotNull(configuration.codecInfo)
            val codecName: String = configuration.codecInfo.name
            TraceUtil.beginSection("createCodec:$codecName")
            val mediaCodec = MediaCodec.createByCodecName(codecName)
            TraceUtil.endSection()
            return mediaCodec
        }
    }

    private var inputByteBuffers: Array<ByteBuffer>? = null
    private var outputByteBuffers: Array<ByteBuffer>? = null

    override fun needsReconfiguration(): Boolean {
        return false
    }

    override fun dequeueInputBufferIndex(): Int {
        return codec.dequeueInputBuffer(0)
    }

    override fun dequeueOutputBufferIndex(bufferInfo: MediaCodec.BufferInfo): Int {
        var index: Int
        do {
            index = codec.dequeueOutputBuffer(bufferInfo, 0)

            if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED && Util.SDK_INT < 21) {
                outputByteBuffers = codec.outputBuffers
            }
        } while (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
        return index
    }

    override fun getOutputFormat(): MediaFormat {
        return codec.outputFormat
    }

    override fun getInputBuffer(index: Int): ByteBuffer? {
        return if (Util.SDK_INT >= 21) {
            codec.getInputBuffer(index)
        } else {
            castNonNull(inputByteBuffers)[index]
        }
    }

    override fun getOutputBuffer(index: Int): ByteBuffer? {
        return if (Util.SDK_INT >= 21) {
            codec.getOutputBuffer(index)
        } else {
            castNonNull(outputByteBuffers)[index]
        }
    }

    /*
    * This is added.
    */
    fun getOutputImage(index: Int): Image? {
        return codec.getOutputImage(index)
    }

    override fun queueInputBuffer(
        index: Int, offset: Int, size: Int, presentationTimeUs: Long, flags: Int
    ) {
        codec.queueInputBuffer(index, offset, size, presentationTimeUs, flags)
    }

    override fun queueSecureInputBuffer(
        index: Int, offset: Int, info: CryptoInfo, presentationTimeUs: Long, flags: Int
    ) {
        codec.queueSecureInputBuffer(
            index, offset, info.frameworkCryptoInfo, presentationTimeUs, flags
        )
    }

    override fun releaseOutputBuffer(index: Int, render: Boolean) {
        codec.releaseOutputBuffer(index, render)
    }

    override fun releaseOutputBuffer(index: Int, renderTimeStampNs: Long) {
        codec.releaseOutputBuffer(index, renderTimeStampNs)
    }

    override fun flush() {
        codec.flush()
    }

    override fun release() {
        inputByteBuffers = null
        outputByteBuffers = null
        codec.release()
    }

    override fun setOnFrameRenderedListener(
        listener: MediaCodecAdapter.OnFrameRenderedListener,
        handler: Handler
    ) {
        codec.setOnFrameRenderedListener(
            { _: MediaCodec?, presentationTimeUs: Long, nanoTime: Long ->
                listener.onFrameRendered(
                    this@ExoplayerCustomMediaCodecAdapter, presentationTimeUs, nanoTime
                )
            },
            handler
        )
    }

    override fun setOutputSurface(surface: Surface) {
        codec.setOutputSurface(surface)
    }

    override fun setParameters(params: Bundle) {
        codec.setParameters(params)
    }

    override fun setVideoScalingMode(@C.VideoScalingMode scalingMode: Int) {
        codec.setVideoScalingMode(scalingMode)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getMetrics(): PersistableBundle {
        return codec.metrics
    }

    init {
        if (Util.SDK_INT < 21) {
            inputByteBuffers = codec.inputBuffers
            outputByteBuffers = codec.outputBuffers
        }
    }
}

