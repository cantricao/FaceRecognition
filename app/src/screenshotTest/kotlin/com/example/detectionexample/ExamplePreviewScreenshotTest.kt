package com.example.detectionexample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.detectionexample.ui.theme.DetectionExampleTheme

class ExamplePreviewScreenshotTest {

    // 1. Screen-level previews across compact, medium, and expanded widths (400, 610, 900 dp) and heights (400, 500, 1000 dp)
    @Preview(name = "Compact-Compact", widthDp = 400, heightDp = 400, showBackground = true)
    @Preview(name = "Compact-Medium", widthDp = 400, heightDp = 500, showBackground = true)
    @Preview(name = "Compact-Expanded", widthDp = 400, heightDp = 1000, showBackground = true)
    @Preview(name = "Medium-Compact", widthDp = 610, heightDp = 400, showBackground = true)
    @Preview(name = "Medium-Medium", widthDp = 610, heightDp = 500, showBackground = true)
    @Preview(name = "Medium-Expanded", widthDp = 610, heightDp = 1000, showBackground = true)
    @Preview(name = "Expanded-Compact", widthDp = 900, heightDp = 400, showBackground = true)
    @Preview(name = "Expanded-Medium", widthDp = 900, heightDp = 500, showBackground = true)
    @Preview(name = "Expanded-Expanded", widthDp = 900, heightDp = 1000, showBackground = true)
    @PreviewTest
    @Composable
    fun ScreenLevelNineSizesPreview() {
        DetectionExampleTheme {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    item(
                        selected = true,
                        onClick = {},
                        icon = {},
                        label = { Text("Camera") }
                    )
                    item(
                        selected = false,
                        onClick = {},
                        icon = {},
                        label = { Text("Video") }
                    )
                }
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Face Recognition Live Preview",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }
    }

    // 2. Screen-level variations: 400x500 with font scale 1.5
    @Preview(name = "FontScale-1.5", widthDp = 400, heightDp = 500, fontScale = 1.5f, showBackground = true)
    @PreviewTest
    @Composable
    fun ScreenLevelFontScalePreview() {
        DetectionExampleTheme {
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Face Recognition with 1.5x Font Scale",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    // 3. Component-level screenshot tests in light theme and with large font
    @Preview(name = "ActionButton-Default", showBackground = true)
    @Preview(name = "ActionButton-LargeFont", fontScale = 1.5f, showBackground = true)
    @PreviewTest
    @Composable
    fun ActionButtonComponentPreview() {
        DetectionExampleTheme {
            Surface(modifier = Modifier.padding(16.dp)) {
                Button(onClick = {}) {
                    Text("Capture Recognition Frame")
                }
            }
        }
    }
}
