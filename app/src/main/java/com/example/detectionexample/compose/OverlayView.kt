package com.example.detectionexample.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.config.Util
import com.example.detectionexample.viewmodels.AnalysisViewModel

@Composable
fun OverlayView(viewModel: AnalysisViewModel = viewModel()) {
    val context = LocalContext.current
    OverlayViewConfig.setContextToFixTextSize(context)
    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())
    Canvas(modifier = OverlayViewConfig.modifier) {
        val bitmap =
            Util.drawBitmapOverlay(trackedObjectsState, size.width.toInt(), size.height.toInt())
        drawImage(
            bitmap.asImageBitmap()
        )
    }
}
