package com.example.detectionexample.wear.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.tooling.preview.RectangularAllWidgetPreviewParams
import androidx.glance.wear.tooling.preview.RoundAllWidgetPreviewParams
import androidx.glance.wear.tooling.preview.SquircleAllWidgetPreviewParams
import androidx.glance.wear.tooling.preview.WearWidgetPreview

/**
 * Previews for FaceRecognitionWearWidget rendered across container shapes.
 */
@Preview(
    name = "Squircle",
    device = "spec:width=1000dp,height=1000dp,dpi=320",
)
@Composable
fun FaceRecognitionWidgetSquirclePreview(
    @PreviewParameter(SquircleAllWidgetPreviewParams::class)
    params: WearWidgetParams,
) {
    WearWidgetPreview(FaceRecognitionWearWidget(), params)
}

@Preview(
    name = "Round",
    device = "spec:width=1000dp,height=1000dp,dpi=320",
)
@Composable
fun FaceRecognitionWidgetRoundPreview(
    @PreviewParameter(RoundAllWidgetPreviewParams::class)
    params: WearWidgetParams,
) {
    WearWidgetPreview(FaceRecognitionWearWidget(), params)
}

@Preview(
    name = "Widget Preview Asset",
    device = "spec:width=1000dp,height=1000dp,dpi=320",
)
@Composable
fun FaceRecognitionWidgetCatalogPreview(
    @PreviewParameter(RectangularAllWidgetPreviewParams::class)
    params: WearWidgetParams,
) {
    WearWidgetPreview(FaceRecognitionWearWidget(), params)
}
