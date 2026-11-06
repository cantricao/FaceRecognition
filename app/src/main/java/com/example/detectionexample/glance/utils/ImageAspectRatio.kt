package com.example.detectionexample.glance.utils

/**
 * Set of aspect ratios generally used for images in a widget.
 * Note that images not in the selected ratio are cropped for display by design.
 */
@JvmInline
value class AspectRatio private constructor(private val value: Int) {
    companion object {
        val Ratio1x1: AspectRatio = AspectRatio(0)
        val Ratio16x9: AspectRatio = AspectRatio(1)
        val Ratio2x3: AspectRatio = AspectRatio(2)
        val Ratio4x3: AspectRatio = AspectRatio(3)

        fun AspectRatio.asDouble(): Double {
            return when (this) {
                Ratio1x1 -> 1.0
                Ratio16x9 -> 16.0 / 9.0
                Ratio2x3 -> 2.0 / 3.0
                Ratio4x3 -> 4.0 / 3.0
                else -> 1.0
            }
        }
    }
}
