package com.example.detectionexample.customexoplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.MediaFormat
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer
import com.example.detectionexample.config.Util
import com.example.detectionexample.viewmodels.DetectionViewModel
import java.nio.ByteBuffer

class VideoView(val context: Context, val viewModel: DetectionViewModel):
    CustomRenderersFactory.VideoFrameDataListener {
    private var frameImageView: ImageView? = null

    fun createPlayer(): ExoPlayer {
        val renderersFactory: CustomRenderersFactory =
            CustomRenderersFactory(context).setVideoFrameDataListener(this)
        return ExoPlayer.Builder(context, renderersFactory).build()
    }

    fun createVideoFrameView(mContext: Context): View {
        frameImageView = ImageView(mContext)
        return frameImageView as ImageView
    }

    override fun onFrame(data: ByteBuffer?, androidMediaFormat: MediaFormat, playerFormat: Format) {
        // Not in main thread
        if (data != null) {
            /*
            * Color formats of different decoders are different.
            * We have to apply different raw-data to Bitmap(argb) conversion systems according to color format.
            * Here we just show YUV to RGB conversion assuming data is YUV formatted.
            * Following conversion system might not give proper result for all videos.
            */
            try {
                val width: Int = playerFormat.width
                val height: Int = playerFormat.height

//                val colorFormat = androidMediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT)

                data.rewind()
                val bytes = ByteArray(data.remaining())
                data.get(bytes)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                Util.yuvToRgb(bytes, bitmap, ImageFormat.YUV_420_888)

                val finalBitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
                frameImageView!!.setImageBitmap(bitmap)
                viewModel.videoAnalyzer.analyze(finalBitmap, System.currentTimeMillis())

                
            } catch (e: Exception) {
                Log.e("TAG", "onFrame: error: " + e.message)
            }
        }
    }

    interface Analyzer {
        fun analyze(image: Bitmap, timestamp: Long)
    }

}