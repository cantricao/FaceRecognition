package com.example.detectionexample.compose.adaptive

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-device preview annotation to verify UI rendering across all canonical form factors:
 * Phone, Foldable, Tablet, and Desktop.
 */
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class PreviewFormFactors

typealias FormFactorPreviews = PreviewFormFactors
