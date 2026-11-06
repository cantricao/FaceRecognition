package com.example.detectionexample.genai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiNanoFaceAnalyzerTest {

    @Test
    fun faceAttributeAnalysis_defaultValues_areEmpty() {
        val analysis = FaceAttributeAnalysis()
        assertEquals("", analysis.expression)
        assertEquals("", analysis.ageGroup)
        assertEquals("", analysis.accessories)
        assertEquals("", analysis.imageQuality)
        assertEquals("", analysis.summary)
    }

    @Test
    fun faceAttributeAnalysis_customValues_preserved() {
        val analysis = FaceAttributeAnalysis(
            expression = "Smiling",
            ageGroup = "Young Adult",
            accessories = "Glasses",
            imageQuality = "Clear",
            summary = "Well-lit portrait of a smiling young adult with glasses."
        )
        assertEquals("Smiling", analysis.expression)
        assertEquals("Young Adult", analysis.ageGroup)
        assertEquals("Glasses", analysis.accessories)
        assertEquals("Clear", analysis.imageQuality)
        assertEquals("Well-lit portrait of a smiling young adult with glasses.", analysis.summary)
    }

    @Test
    fun systemPromptPrefix_exceeds200Words_forPrefixCachingOptimization() {
        val prefix = GeminiNanoFaceAnalyzer.SYSTEM_PROMPT_PREFIX.trim()
        val wordCount = prefix.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        assertTrue(
            "SYSTEM_PROMPT_PREFIX must be over 200 words to meet prefix caching optimization criteria (actual: $wordCount words)",
            wordCount >= 200
        )
    }

    @Test
    fun geminiNanoStatus_sealedTypes_behaveCorrectly() {
        val available: GeminiNanoStatus = GeminiNanoStatus.Available
        val checking: GeminiNanoStatus = GeminiNanoStatus.Checking
        val downloadable: GeminiNanoStatus = GeminiNanoStatus.Downloadable(progressBytes = 1024L)
        val downloading: GeminiNanoStatus = GeminiNanoStatus.Downloading
        val unavailable: GeminiNanoStatus = GeminiNanoStatus.Unavailable
        val error: GeminiNanoStatus = GeminiNanoStatus.Error("AICore not installed")

        assertEquals(GeminiNanoStatus.Available, available)
        assertEquals(GeminiNanoStatus.Checking, checking)
        assertEquals(1024L, (downloadable as GeminiNanoStatus.Downloadable).progressBytes)
        assertEquals(GeminiNanoStatus.Downloading, downloading)
        assertEquals(GeminiNanoStatus.Unavailable, unavailable)
        assertEquals("AICore not installed", (error as GeminiNanoStatus.Error).message)
    }

    @Test
    fun analyzer_close_canBeInvokedSafely() {
        val analyzer = GeminiNanoFaceAnalyzer()
        analyzer.close() // Should not throw
        assertNotNull(analyzer)
    }
}
