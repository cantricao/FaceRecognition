package com.example.detectionexample.record

import android.graphics.Bitmap
import android.media.*
import android.media.MediaCodecInfo.CodecCapabilities
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

//from https://stackoverflow.com/questions/17096726/how-to-encode-bitmaps-into-a-video-using-mediacodec
class BitmapToVideoEncoder(private val mCallback: IBitmapToVideoEncoderCallback) {
    private var mOutputFile: File? = null
    private var mEncodeQueue: Queue<Bitmap?> = ConcurrentLinkedQueue<Bitmap?>()
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private val mFrameSync = Any()
    private var mNewFrameLatch: CountDownLatch? = null
    private var mGenerateIndex = 0
    private var mTrackIndex = 0
    private var mNoMoreFrames = true
    private var mAbort = false

    interface IBitmapToVideoEncoderCallback {
        fun onEncodingComplete(outputFile: File?)
    }

    val isEncodingStarted: Boolean
        get() = mediaCodec != null && mediaMuxer != null && !mNoMoreFrames && !mAbort

    fun getActiveBitmaps(): Int {
        return mEncodeQueue.size
    }

    fun startEncoding(width: Int, height: Int, outputFile: File) {
        mWidth = width
        mHeight = height
        mOutputFile = outputFile
        val outputFileString: String = try {
            outputFile.canonicalPath
        } catch (e: IOException) {
            Log.e(TAG, "Unable to get path for $outputFile")
            return
        }
        val codecInfo = selectCodec(MIME_TYPE)
        if (codecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for $MIME_TYPE")
            return
        }
        Log.d(TAG, "found codec: " + codecInfo.name)
        val colorFormat: Int = try {
            selectColorFormat(codecInfo, MIME_TYPE)
        } catch (e: Exception) {
            CodecCapabilities.COLOR_FormatYUV420Flexible
        }
        mediaCodec = try {
            MediaCodec.createByCodecName(codecInfo.name)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to create MediaCodec " + e.message)
            return
        }
        val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec!!.start()
        try {
            mediaMuxer = MediaMuxer(outputFileString, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//            mediaMuxer!!.setOrientationHint(270)
        } catch (e: IOException) {
            Log.e(TAG, "MediaMuxer creation failed. " + e.message)
            return
        }
        Log.d(TAG, "Initialization complete. Starting encoder...")
        mNoMoreFrames = false
        val handlerThread = HandlerThread("renderer")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.post { encode() }
    }

    fun stopEncoding() {
        if (mediaCodec == null || mediaMuxer == null) {
            Log.d(TAG, "Failed to stop encoding since it never started")
            return
        }
        Log.d(TAG, "Stopping encoding")
        mNoMoreFrames = true
        mEncodeQueue = ConcurrentLinkedQueue<Bitmap?>() // Drop all frames
        synchronized(mFrameSync) {
            if (mNewFrameLatch != null && mNewFrameLatch!!.count > 0) {
                mNewFrameLatch!!.countDown()
            }
        }
    }

    fun abortEncoding() {
        if (mediaCodec == null || mediaMuxer == null) {
            Log.d(TAG, "Failed to abort encoding since it never started")
            return
        }
        Log.d(TAG, "Aborting encoding")
        mNoMoreFrames = true
        mAbort = true
        mEncodeQueue = ConcurrentLinkedQueue<Bitmap?>() // Drop all frames
        synchronized(mFrameSync) {
            if (mNewFrameLatch != null && mNewFrameLatch!!.count > 0) {
                mNewFrameLatch!!.countDown()
            }
        }
    }

    fun queueFrame(frame: Bitmap?) {
        if (mediaCodec == null || mediaMuxer == null) {
            Log.d(TAG, "Failed to queue frame. Encoding not started")
            return
        }
        Log.d(TAG, "Queueing frame")
        mEncodeQueue.add(frame)
        synchronized(mFrameSync) {
            if (mNewFrameLatch != null && mNewFrameLatch!!.count > 0) {
                mNewFrameLatch!!.countDown()
            }
        }
    }

    private fun encode() {
        Log.d(TAG, "Encoder started")
        while (!mNoMoreFrames) {
//            if (mNoMoreFrames && mEncodeQueue.size == 0) break
            var bitmap = mEncodeQueue.poll()
            if (bitmap == null) {
                synchronized(mFrameSync) { mNewFrameLatch = CountDownLatch(1) }
                try {
                    mNewFrameLatch!!.await()
                } catch (e: InterruptedException) {
                    Log.e(TAG, e.message!!)
                }
                bitmap = mEncodeQueue.poll()
            }
            if (bitmap == null) continue
            val byteConvertFrame = getNV21(bitmap.width, bitmap.height, bitmap)

            val inputBufIndex = mediaCodec!!.dequeueInputBuffer(TIMEOUT_USEC)
            val ptsUsec = computePresentationTime(mGenerateIndex.toLong(), FRAME_RATE)
            if (inputBufIndex >= 0) {
                val inputBuffer = mediaCodec!!.getInputBuffer(inputBufIndex)
                inputBuffer!!.clear()
                inputBuffer.put(byteConvertFrame)
                mediaCodec!!.queueInputBuffer(inputBufIndex, 0, byteConvertFrame.size, ptsUsec, 0)
                mGenerateIndex++
            }
            val mBufferInfo = MediaCodec.BufferInfo()
            val encoderStatus = mediaCodec!!.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                Log.e(TAG, "No output from encoder available")
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d(TAG, "encoderStatus MediaCodec.INFO_OUTPUT_FORMAT_CHANGED")
                // not expected for an encoder
                val newFormat = mediaCodec!!.outputFormat
                mTrackIndex = mediaMuxer!!.addTrack(newFormat)
                mediaMuxer!!.start()
            } else if (encoderStatus < 0) {
                Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
            } else if (mBufferInfo.size != 0) {
                val encodedData = mediaCodec!!.getOutputBuffer(encoderStatus)
                if (encodedData == null) {
                    Log.e(TAG, "encoderOutputBuffer $encoderStatus was null")
                } else {
                    encodedData.position(mBufferInfo.offset)
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size)
                    mediaMuxer!!.writeSampleData(mTrackIndex, encodedData, mBufferInfo)
                    mediaCodec!!.releaseOutputBuffer(encoderStatus, false)
                }
            }
        }
        release()
        if (mAbort) {
            mOutputFile!!.delete()
        } else {
            mCallback.onEncodingComplete(mOutputFile)
        }
    }

    private fun release() {
        if (mediaCodec != null) {
            mediaCodec!!.stop()
            mediaCodec!!.release()
            mediaCodec = null
            Log.d(TAG, "RELEASE CODEC")
        }
        if (mediaMuxer != null) {
            mediaMuxer!!.stop()
            mediaMuxer!!.release()
            mediaMuxer = null
            Log.d(TAG, "RELEASE MUXER")
        }
    }

    private fun getNV21(inputWidth: Int, inputHeight: Int, scaled: Bitmap): ByteArray {
        val argb = IntArray(inputWidth * inputHeight)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
        scaled.recycle()
        return yuv
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var a: Int
        var r: Int
        var g: Int
        var b: Int
        var y: Int
        var u: Int
        var v: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                a = argb[index] and -0x1000000 shr 24 // a is not used obviously
                r = argb[index] and 0xff0000 shr 16
                g = argb[index] and 0xff00 shr 8
                b = argb[index] and 0xff shr 0
                y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
                index++
            }
        }
    }

    private fun computePresentationTime(frameIndex: Long, framerate: Int): Long {
        return 132 + frameIndex * 1000000 / framerate
    }

    companion object {
        private val TAG = BitmapToVideoEncoder::class.java.simpleName
        private const val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        private var mWidth = 0
        private var mHeight = 0
        private const val BIT_RATE = 16000000
        private const val FRAME_RATE = 30 // Frames per second
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_USEC: Long = 500000
        private fun selectCodec(mimeType: String): MediaCodecInfo? {
            val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            for (codecInfo in codecInfos) {
                if (!codecInfo.isEncoder) {
                    continue
                }
                val types = codecInfo.supportedTypes
                for (type in types) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        return codecInfo
                    }
                }
            }
            return null
        }

        private fun selectColorFormat(
            codecInfo: MediaCodecInfo,
            mimeType: String
        ): Int {
            val capabilities = codecInfo
                .getCapabilitiesForType(mimeType)
            for (colorFormat in capabilities.colorFormats) {
                if (isRecognizedFormat(colorFormat)) {
                    Log.d(TAG, colorFormat.toString())
                    return colorFormat
                }
            }
            return 0 // not reached
        }

        private fun isRecognizedFormat(colorFormat: Int): Boolean {
            return when (colorFormat) {
                CodecCapabilities.COLOR_FormatYUV420PackedPlanar, CodecCapabilities.COLOR_FormatYUV420SemiPlanar, CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
                else -> false
            }
        }
    }
}